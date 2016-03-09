package com.github.api.service

import java.time.Instant

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.api.domain._
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsSuccess, JsValue, Json}
import spray.can.Http
import spray.http.HttpMethods._
import spray.http.Uri.Path
import spray.http.{HttpRequest, HttpResponse, _}

import scala.concurrent.Future
import scala.concurrent.duration._

class GitHubApiService(actorSystem: ActorSystem) extends LazyLogging {

  import actorSystem.dispatcher

  implicit val system = actorSystem
  implicit val timeout: Timeout = Timeout(15.seconds)

  type RepositoriesWithContributors = Seq[(Repository, Seq[Contributor])]

  def fetchRepositories(name: String): Future[RepositoriesWithContributors] = {
    val requestForRepositories = GitHubApiService.buildRequestForRepositories(name)

    get(requestForRepositories) flatMap { resp =>
      resp.status match {
        case StatusCodes.OK =>
          val json = asJsonSeq(resp.entity.asString)
          val repositories = GitHubApiService.asRepository(json)
          val reposWithContributors = repositories map (x => findContributors(x).map(y => x -> y))
          Future.sequence(reposWithContributors).map(x => x)

        case other => sys.error(s"received $other status with response: $resp for request $requestForRepositories ")
      }
    }
  }

  private def findContributors(repository: Repository): Future[Seq[Contributor]] = {
    val contributions = contributionsFor(repository).map[Seq[Contribution]] { resp =>
      val json = asJsonSeq(resp.entity.asString)
      json flatMap { value =>
        for {
          contributionsAmount <- (value \ "contributions").asOpt[Int]
          accountUrl <- (value \ "url").asOpt[String]
        } yield Contribution(contributionsAmount, accountUrl)
      }
    }

    val values = contributions.map { x =>
      val mapped = x.map { contribution =>
        val accountInfo = findAccountInfo(contribution)
        accountInfo
      }
      Future.sequence(mapped).map(contributors => contributors.flatten)
    }

    values.flatMap(contributorsFuture => contributorsFuture)
  }

  protected def contributionsFor(repository: Repository): Future[HttpResponse] = {
    get(GitHubApiService.buildRequest(repository.contributorsUrl))
  }

  private def findAccountInfo(contribution: Contribution): Future[Option[Contributor]] = {
    contributorFor(contribution).map[Option[Contributor]] { resp =>
      val accountJson = Json.parse(resp.entity.asString)
      for {
        name <- (accountJson \ "name").asOpt[String]
        followers <- (accountJson \ "followers").asOpt[Int]
        created <- (accountJson \ "created_at").asOpt[Instant]
      } yield Contributor(fullName = name, created = created, contributions = contribution.contributions, followers = followers)
    }
  }

  protected def contributorFor(contribution: Contribution): Future[HttpResponse] = {
    get(GitHubApiService.buildRequest(contribution.accountUrl))
  }

  protected def get(request: HttpRequest): Future[HttpResponse] = {
    (IO(Http) ? request).mapTo[HttpResponse]
  }

  private def asJsonSeq(value: String): Seq[JsValue] = Json.parse(value).as[Seq[JsValue]]

}

object GitHubApiService {
  val Url = "https://api.github.com"
  val GetRepositories = "/orgs/:org/repos"

  def buildRequestForRepositories(organizationName: String): HttpRequest = {
    HttpRequest(method = GET, uri = Uri(Url)
      .withPath(Path(GetRepositories.replace(":org", organizationName))))
  }

  def buildRequest(uri: String): HttpRequest = {
    HttpRequest(method = GET, uri = Uri(uri))
  }

  def asRepository(values: Seq[JsValue]): Seq[Repository] = {
    val repositories = values map { json =>
      for {
        name <- (json \ "name").validate[String]
        contributorsUrl <- (json \ "contributors_url").validate[String]
      } yield Repository(name, contributorsUrl)
    }
    repositories collect { case x: JsSuccess[Repository] => x.value.asInstanceOf[Repository] }
  }
}
