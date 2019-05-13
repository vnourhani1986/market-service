package com.snapptrip.utils

import akka.http.scaladsl.model.StatusCode

/**
  * Container for custom http error handling
  *
  * @param originalCause
  * @param status
  */

object ErrorCodes {
  val CANCELLATION_FAILED = 4500
  val GENERAL_ERROR_CODE = 5000
  val USER_IS_NOT_VALID = 4501
  val USER_IS_DISABLED = 4502
  val USER_EXIST = 4502
}

case class HttpError(result: String, errorCode: Option[Int], message: String)

case class ExtendedException(cause: String, errorCode: Option[Int], statusCode: StatusCode) extends Throwable

class CancellationException(cause: String, errorCode: Option[Int], statusCode: StatusCode ) extends ExtendedException(cause: String, errorCode: Option[Int], statusCode: StatusCode)
class CompensationException(cause: String, errorCode: Option[Int], statusCode: StatusCode ) extends ExtendedException(cause: String, errorCode: Option[Int], statusCode: StatusCode )
class SnapptripAuthGetUserException(cause: String, errorCode: Option[Int], statusCode: StatusCode) extends ExtendedException(cause: String, errorCode: Option[Int], statusCode: StatusCode)


class OnlineRefundException(cause: String, errorCode: Option[Int], statusCode: StatusCode) extends ExtendedException(cause: String, errorCode: Option[Int], statusCode: StatusCode)

class UserException(cause: String, errorCode: Option[Int], statusCode: StatusCode) extends ExtendedException(cause: String, errorCode: Option[Int], statusCode: StatusCode)


