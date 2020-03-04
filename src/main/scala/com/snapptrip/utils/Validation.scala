package com.snapptrip.utils

import com.snapptrip.utils.formatters.MobileNoFormatter.isNumber

trait Validation {

  def validation(emailOpt: Option[String], mobileNoOpt: Option[String]): String = {

    val mobileNo = mobileNoOpt.getOrElse("")
    val email = emailOpt.getOrElse("")
    val isValidMobile = isNumber(mobileNo)
    val isValidEmail = EmailAddress.isValid(email)

    if (email.isEmpty && mobileNo.isEmpty) {
      "one of the fields of mobile or email need to be defined"
    }
    else if (email.nonEmpty && !isValidEmail && mobileNo.nonEmpty && !isValidMobile) {
      "invalid Email and mobile number"
    }
    else if (email.nonEmpty && !isValidEmail) {
      "invalid Email"
    }
    else if (mobileNo.nonEmpty && !isValidMobile) {
      "invalid mobile number"
    }
    else ""

  }

}
