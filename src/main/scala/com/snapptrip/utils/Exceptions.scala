package com.snapptrip.utils

import akka.actor.ActorRef

/**
  * Container for custom http error handling
  *
  * @param originalCause
  * @param status
  */

object Exceptions {

  object ErrorCodes {
    val DatabaseError = 5011
    val BadRequestError = 4000
    val AuthenticationError = 4010
    val InvalidURL = 4040
    val RestServiceError = 5012
    val JsonParseError = 4011
    val TimeFormatError = 4012
    val DatabaseQueryError = 5012
    val InternalSeverError = 5000
  }

  case class HttpError(result: String, errorCode: Option[Int], message: String)

  case class ExtendedException(message: String, errorCode: Int, ref: ActorRef = null) extends Throwable


}



