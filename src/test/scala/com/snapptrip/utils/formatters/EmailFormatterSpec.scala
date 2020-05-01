package com.snapptrip.utils.formatters

import org.scalatest.{Matchers, WordSpecLike}

class EmailFormatterSpec
  extends WordSpecLike
    with Matchers {

  "email formatter function" must {

    "return input email when email is valid" in {

      EmailFormatter.format(Some("test@pintapin.com")) shouldBe Some("test@pintapin.com")

    }
    "return None for none email format" in {

      EmailFormatter.format(Some("pintapin.com")) shouldBe None
      EmailFormatter.format(Some("test@pintapin")) shouldBe None
      EmailFormatter.format(Some("test@pint?a/pin.com")) shouldBe None

    }
  }

}
