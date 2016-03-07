package com.github.api.service

import java.time.Instant

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.api.service.GitHubApiService.{Contributor, Repository}
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
  implicit val timeout: Timeout = Timeout(5.seconds)

  type ReposWithContributors = Seq[(Repository, Seq[Contributor])]

  def fetchRepositories(name: String): Future[ReposWithContributors] = {
    val requestForRepositories = GitHubApiService.buildRequestForRepositories(name)

    get(requestForRepositories) flatMap { resp =>
      val json = asJsonSeq(resp.entity.asString)
      val repositories = GitHubApiService.asRepository(json)

      val reposWithContributors = repositories map (x => findContributors(x).map(y => x -> y))

      Future.sequence(reposWithContributors).map(x => x)
    }
  }

  private def findContributors(repository: Repository): Future[Seq[Contributor]] = {
    get(GitHubApiService.buildRequest(repository.contributorsUrl)).map[Seq[Contributor]] { resp =>
      val json = asJsonSeq(resp.entity.asString)
      json flatMap { value =>
        for {
          contributionsAmount <- (value \ "contributions").asOpt[Int]
          login <- (value \ "login").asOpt[String]

        } yield Contributor(
          login = login,
          fullName = "",
          created = Instant.now(),
          contributions = contributionsAmount,
          followers = 0)
      }
    }
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

  case class Repository(name: String, contributorsUrl: String)

  case class Contributor(
                          login: String,
                          fullName: String,
                          created: Instant,
                          contributions: Int,
                          followers: Int,
                          influence: BigDecimal = 0)


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
