package com.github.api.actor

import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.github.api.actor.GitHubClientActor.{Evaluate, Lookup}
import com.github.api.domain.{Contributor, Repository}
import com.github.api.service.{EvaluationUtil, GitHubApiService}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.Future


class GitHubClientActorSpec
  extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll
    with Eventually {


  override def afterAll {
    TestKit.shutdownActorSystem(system)

    "GitHubClientActor" should {
      "Perform organization lookup" in new TestScope {
        githubActor ! Lookup("typesafe")
        eventually(timeout(3.seconds)) {
          verify(mockedService.fetchRepositories("typesafe"), times(1))
        }
      }

      "Perform evaluation" in new TestScope {
        githubActor ! Evaluate(reposWithContributors.toMap)
        eventually(timeout(3.seconds)) {
          verify(mockedEvaluationUtil).evaluateByInfluence(any[Seq[Contributor]])
        }
      }
    }
  }

  trait TestScope {
    val repo = Repository(name = "typeSafe", contributorsUrl = "typesafeContributors")
    val contributors = Seq(Contributor(
      fullName = "Hero",
      created = Instant.now(),
      contributions = 10,
      followers = 15
    ))
    val reposWithContributors: Seq[(Repository, Seq[Contributor])] = Seq((repo, contributors))

    val mockedService = mock[GitHubApiService]
    when(mockedService.fetchRepositories("typesafe")) thenReturn Future.successful(reposWithContributors)

    val mockedEvaluationUtil = new EvaluationUtil

    val githubActor = TestActorRef(new TestGithubActor(mockedService))

    class TestGithubActor(gitHubApiService: GitHubApiService) extends GitHubClientActor(gitHubApiService: GitHubApiService) {
      override lazy val evaluationUtil: EvaluationUtil = mockedEvaluationUtil
    }
  }

}

