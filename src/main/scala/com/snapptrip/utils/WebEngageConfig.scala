package com.snapptrip.utils

import akka.http.scaladsl.settings.ClientConnectionSettings
import com.snapptrip.DI._
import scala.concurrent.duration._

object WebEngageConfig {
  val host: String = config.getString("web-engage.host")
  private val baseUrl: String = config.getString("web-engage.api-base-url")
  private val userUrl: String = config.getString("web-engage.user-url")
  private val eventUrl: String = config.getString("web-engage.event-url")
  private val opengdprRequestUrl: String = config.getString("web-engage.opengdpr-requests-url")
  private val licenseCode: String = config.getString("web-engage.license-code")
  val apiKey: String = config.getString("web-engage.api-key")
  val usersUrl: String = s"""$baseUrl$licenseCode$userUrl"""
  val eventsUrl: String = s"""$baseUrl$licenseCode$eventUrl"""
  val opengdprRequestsUrl: String = s"""$baseUrl$licenseCode$opengdprRequestUrl"""
  val clientConnectionSettings: ClientConnectionSettings = ClientConnectionSettings(system).withConnectingTimeout(180.second)
  val timeOffset: String = config.getString("web-engage.time-offset")
}
