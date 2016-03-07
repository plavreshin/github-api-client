package com.github.api.domain

import java.time.Instant

case class UserDetails(
  login: String,
  fullName: String,
  created: Instant,
  followers: Seq[String]) {

}