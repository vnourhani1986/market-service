package com.snapptrip.utils.formatters

import org.scalatest.{Matchers, WordSpecLike}

class MobileNoFormatterSpec
  extends WordSpecLike
    with Matchers {

  "mobile number formatter function" must {

    "return input mobile number when is valid" in {

      MobileNoFormatter.format(Some("9124497405")) shouldBe Some("9124497405")
      MobileNoFormatter.format(Some("09124497405")) shouldBe Some("9124497405")
      MobileNoFormatter.format(Some("00989124497405")) shouldBe Some("9124497405")
      MobileNoFormatter.format(Some("+989124497405")) shouldBe Some("9124497405")

    }
    "return None for none email format" in {

      MobileNoFormatter.format(Some("91244974051")) shouldBe None
      MobileNoFormatter.format(Some("1191244974051")) shouldBe None

    }
  }

}
