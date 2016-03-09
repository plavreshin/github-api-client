package com.github.api.service

import java.time.{Instant, LocalDate, ZoneOffset}

import com.github.api.domain.Contributor
import org.scalatest.{Matchers, WordSpec}

class EvaluationUtilSpec extends WordSpec with Matchers {
  "Evaluation Util" should {
    "evaluate seq of contributors and sort them by influence " in {
      val fresh = Contributor(
        fullName = "Hero",
        created = date2Instant(1L),
        contributions = 10,
        followers = 15
      )
      val old = Contributor(
        fullName = "Stranger",
        created = date2Instant(3L),
        contributions = 100,
        followers = 400)

      val contributors = Seq(fresh, old)

      val evaluated = EvaluationUtil.evaluateByInfluence(contributors = contributors)

      val topOne = evaluated.head
      topOne.fullName shouldBe old.fullName
    }
  }

  def date2Instant(years: Long): Instant = {
    LocalDate.now().minusYears(years).atStartOfDay().toInstant(ZoneOffset.UTC)
  }
}
