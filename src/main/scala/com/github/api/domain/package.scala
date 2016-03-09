package com.github.api

import java.time.Instant

package object domain {
  case class Contributor(
    fullName: String,
    created: Instant,
    contributions: Int = 0,
    followers: Int = 0,
    influence: BigDecimal = 0)

  case class Repository(name: String, contributorsUrl: String)

  case class Contribution(contributions: Int, accountUrl: String)

}
