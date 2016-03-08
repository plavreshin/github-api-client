package com.github.api.actor

import akka.actor.{Actor, Props}
import com.github.api.actor.GitHubClientActor.{Calculate, In, Lookup}
import com.github.api.service.{CalculationUtil, GitHubApiService}
import com.github.api.service.GitHubApiService.{Contributor, Repository}
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}

class GitHubClientActor(gitHubApiService: GitHubApiService) extends Actor with LazyLogging {

  import context.dispatcher

  override def receive: Receive = {
    case msg: In => msg match {
      case x: Lookup =>
        gitHubApiService.fetchRepositories(x.name) onComplete {
          case Success(result) =>
            val reposWithContributors = result
            logger.info(s"Scheduling calculation for : $reposWithContributors")
            self ! Calculate(reposWithContributors.toMap)
          case Failure(f) =>
            logger.error("Lookup failed with:", f)
        }

      case x: Calculate =>
        logger.info(s"Received calculation: $x")
        val evaluated = x.results.map { case (repo, contributors) =>
          repo -> CalculationUtil.evaluateByInfluence(contributors)
        }
        evaluated.foreach { case (repo, contributors) =>
          logger.info(s"Repo name: ${repo.name} and it's contributors: $contributors")
        }
    }
  }
}

object GitHubClientActor {

  def props(gitHubApiService: GitHubApiService): Props = Props(classOf[GitHubClientActor], gitHubApiService)

  sealed trait In

  case class Lookup(name: String) extends In

  case class Calculate(results: Map[Repository, Seq[Contributor]]) extends In

}
