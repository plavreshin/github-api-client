package com.github.api.service

import java.time.{LocalDate, Period}

import com.github.api.service.GitHubApiService.Contributor

object CalculationUtil {

  def evaluateByInfluence(contributors: Seq[Contributor]): Seq[Contributor] = {
    def findInfluence(commits: Int, followers: Int, accountAge: Int) = {
      (commits * 0.1) + (followers * 0.5) + accountAge
    }

    def years(x: Contributor): Int = {
      Period.between(LocalDate.from(x.created), LocalDate.now()).getYears
    }

    contributors.map { x =>
      val influence = findInfluence(x.contributions, x.followers, years(x))
      x.copy(influence = influence)
    }.sortWith(_.influence > _.influence)
  }

}
