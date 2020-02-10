package com.snapptrip.utils

import akka.http.scaladsl.settings.ClientConnectionSettings
import com.snapptrip.DI._

import scala.concurrent.duration._

object NotificationConfig {
  val host: String = config.getString("notification.host")
  private val baseURL: String = config.getString("notification.api-base-url")
  private val sms: String = config.getString("notification.sms-url")
  private val email: String = config.getString("notification.email-url")
  val emailUrl = s"""$baseURL$email"""
  val smsUrl = s"""$baseURL$sms"""
  val client: String = config.getString("notification.client")
  val port: Int = config.getInt("notification.port")
  val clientConnectionSettings: ClientConnectionSettings = ClientConnectionSettings(system).withConnectingTimeout(15.second)
}
