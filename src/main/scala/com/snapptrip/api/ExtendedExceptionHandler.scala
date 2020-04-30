package com.snapptrip.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.snapptrip.formats.JsonProtocol
import com.snapptrip.utils.Exceptions.ExtendedException
import com.snapptrip.utils.SentryClient

/**
  * Custom Akka Http error handler. Courtesy of cut.social.
  */
object ExtendedExceptionHandler extends JsonProtocol {

  def handle()(implicit logger: com.typesafe.scalalogging.Logger): ExceptionHandler = {
    ExceptionHandler {
      case t@ExtendedException(content, errorCode) =>
        SentryClient.log(t, None)
        logger.error(content, t)
        complete(HttpResponse(status = 500, entity = content))
      case error: Throwable =>
        SentryClient.log(error, None)
        logger.error(s"Error while handling", error)
        complete("" /*HttpError("failed", Some(ErrorCodes.GENERAL_ERROR_CODE), error.getMessage)*/)
    }

  }

}
