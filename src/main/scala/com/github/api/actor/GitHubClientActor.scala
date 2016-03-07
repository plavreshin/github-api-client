package com.github.api.actor

import akka.actor.{Actor, Props}
import com.github.api.actor.GitHubClientActor.{Calculate, In, Lookup}
import com.github.api.service.GitHubApiService
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}

class GitHubClientActor(gitHubApiService: GitHubApiService) extends Actor with LazyLogging {

  import context.dispatcher

  override def receive: Receive = {
    case msg: In => msg match {
      case x: Lookup =>
        gitHubApiService.fetchRepositories(x.name) onComplete {
          case Success(s) =>
            logger.info(s"Scheduling calculation: $s")
            self ! Calculate(Seq())
          case Failure(f) =>
            logger.error("Lookup failed with:", f)
        }

      case x: Calculate =>
        logger.info(s"Received calculation: $x")
    }
  }
}

object GitHubClientActor {

  def props(gitHubApiService: GitHubApiService): Props = Props(classOf[GitHubClientActor], gitHubApiService)

  sealed trait In

  case class Lookup(name: String) extends In

  case class Calculate(results: Seq[String]) extends In

}
