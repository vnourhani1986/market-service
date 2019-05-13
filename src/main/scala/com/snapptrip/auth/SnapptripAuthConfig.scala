package com.snapptrip.auth

import com.snapptrip.DI._


object SnapptripAuthConfig {

  val apiBaseUrl: String = config.getString("snapptrip-auth.api-base-url")
  val loginUrl: String = config.getString("snapptrip-auth.login-url")
  val authUrl: String = config.getString("snapptrip-auth.auth-url")
  val port: Int = config.getInt("snapptrip-auth.port")
}
