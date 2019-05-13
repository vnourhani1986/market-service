package com.snapptrip.utils

import akka.http.scaladsl.settings.ClientConnectionSettings
import com.snapptrip.DI._

import scala.concurrent.duration._

object NotificationConfig {
  val host: String = config.getString("notification.host")
  val baseURL: String = config.getString("notification.api-base-url")
  val clientConnectionSettings: ClientConnectionSettings = ClientConnectionSettings(system).withConnectingTimeout(30.second)
}
