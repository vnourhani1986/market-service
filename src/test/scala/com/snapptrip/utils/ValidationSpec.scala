package com.snapptrip.utils

import org.scalatest.{Matchers, WordSpecLike}

class ValidationSpec
  extends WordSpecLike
    with Matchers
    with Validation {

  "validation function" must {

    "validate email and mobile" in {

      val email = Some("test@pintapin.com")
      val mobileNo = Some("9121000000")

      assert(validation(email, mobileNo) == "")
    }

  }

}
