package com.github.api.domain

import play.api.libs.json.Json

object Formats {
  implicit val contributorsFormat = Json.format[Contributor]
}
