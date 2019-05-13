package com.snapptrip.utils


import io.sentry.Sentry
import io.sentry.event.interfaces.ExceptionInterface
import io.sentry.event.{Event, EventBuilder}
import com.snapptrip.DI._

object SentryClient {

  private val sentryDns: String = config.getString("sentry.dns")
  private val sentryEnvironment: String = config.getString("sentry.environment")

  private val sentry = Sentry.init(sentryDns)

  def log(e: Throwable, message: Option[String] = None): Unit = {
    val eventBuilder = new EventBuilder()
    val event = eventBuilder
      .withMessage(message.getOrElse(""))
      .withLevel(Event.Level.ERROR)
      .withSentryInterface(new ExceptionInterface(e))
        .withEnvironment(sentryEnvironment)
    sentry.sendEvent(event)

  }

}
