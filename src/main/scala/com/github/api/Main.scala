package com.github.api

import akka.actor.ActorSystem
import com.github.api.actor.GitHubClientActor
import com.github.api.actor.GitHubClientActor.Lookup
import com.github.api.service.GitHubApiService
import com.typesafe.scalalogging.LazyLogging

object Main extends App with LazyLogging {

  val lookupValue: Option[String] = args.find(_.startsWith("-Dlookup.value="))

  logger.info(s"Starting GitHubClient with to find all commiters for $lookupValue")

  implicit lazy val system = ActorSystem("GitHubSystem")

  lazy val gitHubApiService = new GitHubApiService(system)

  val clientActor = system.actorOf(GitHubClientActor.props(gitHubApiService))

  clientActor ! Lookup("typesafehub")
}
