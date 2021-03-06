package com.github.api.actor

import akka.actor.{Actor, Props}
import com.github.api.actor.GitHubClientActor.{Evaluate, In, Lookup}
import com.github.api.domain.{Contributor, Repository}
import com.github.api.service.{EvaluationUtil, GitHubApiService}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import scala.util.{Failure, Success}

class GitHubClientActor(gitHubApiService: GitHubApiService) extends Actor with LazyLogging {

  import context.dispatcher

  import com.github.api.domain.Formats.contributorsFormat

  lazy val evaluationUtil = new EvaluationUtil

  override def receive: Receive = {
    case msg: In => msg match {
      case x: Lookup =>
        gitHubApiService.fetchRepositories(x.name) onComplete {
          case Success(result) =>
            val reposWithContributors = result
            logger.info(s"Scheduling calculation for : $reposWithContributors")
            self ! Evaluate(reposWithContributors.toMap)
          case Failure(f) =>
            logger.error("Lookup failed with:", f)
        }

      case x: Evaluate =>
        val evaluated = x.results.map { case (repo, contributors) =>
          repo -> evaluationUtil.evaluateByInfluence(contributors)
        }
        evaluated.foreach { case (repo, contributors) =>
          logger.info(s"Repo name: ${repo.name} and it's contributors: ${Json.prettyPrint(Json.toJson(contributors))}")
        }
    }
  }
}

object GitHubClientActor {

  def props(gitHubApiService: GitHubApiService): Props = Props(classOf[GitHubClientActor], gitHubApiService)

  sealed trait In

  case class Lookup(name: String) extends In

  case class Evaluate(results: Map[Repository, Seq[Contributor]]) extends In

}
