package com.snapptrip.utils

import spray.json._

object Errors extends DefaultJsonProtocol {

  def exception(errorCode: String): Exception = {
    new Exception("اجرای درخواست شما با خطا مواجه شده است. لطفاً دقایقی دیگر درخواست خود را دوباره ارائه نمایید.")
  }

}


