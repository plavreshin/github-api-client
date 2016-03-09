package com.github.api.service

import java.time._
import java.time.temporal.ChronoUnit

import com.github.api.domain.Contributor

class EvaluationUtil {

  def evaluateByInfluence(contributors: Seq[Contributor]): Seq[Contributor] = {
    def findInfluence(commits: Int, followers: Int, accountAge: Int) = {
      (commits * 0.1) + (followers * 0.5) + accountAge
    }

    def years(x: Contributor): Int = {
      ChronoUnit.YEARS.between(LocalDateTime.ofInstant(x.created, ZoneOffset.ofHours(0)), LocalDateTime.now()).toInt
    }

    contributors.map { x =>
      val influence = findInfluence(x.contributions, x.followers, years(x))
      x.copy(influence = influence)
    }.sortWith(_.influence > _.influence)
  }

}
