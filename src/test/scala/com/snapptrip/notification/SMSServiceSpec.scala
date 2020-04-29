package com.snapptrip.notification

import akka.http.scaladsl.model.StatusCodes
import akka.testkit.{ImplicitSender, TestKit}
import com.snapptrip.DI.{ec, system}
import com.snapptrip.api.Messages.WebEngageSMSBody
import com.snapptrip.formats.Formats._
import com.snapptrip.notification.sms.SmsService
import com.snapptrip.service.actor.StopSystemAfterAll
import org.scalatest.{Matchers, WordSpecLike}
import spray.json.JsonParser

class SMSServiceSpec
  extends TestKit(system)
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with StopSystemAfterAll {

  "An webengage actor" must {

    "send back messages :" in {

      val bodyJson =
        s"""{
          "version": "1.0",
          "smsData" :{
              "toNumber" : "9124497404",
              "fromNumber" : "snapptrip",
              "body" : "hi snapptrip"
          },
          "metadata": {
              "campaignType": "PROMOTIONAL",
              "timestamp": "2019-06-18T06:45:12+0000",
              "messageId": "atFdWpti/1G1MB1ejetQdxdPwjbtX2wLIjV/13B30nxwKRnF0cNnoD/OGnltEydBJ6+fyicGLQkxpZUBKwokWlwlNROEfPICFH6KBjFMUjcGkic77Pi8ExKq1mgG4/WTPruNxnobeMNRj/0gWB/XMkA/kDySVwWKqfoOVsnWaFv4EnDcJitlIz0b2DT7Jp1berdtWhUKBTi7VuFqMUiqjjMo/UdGNM7SzQHTab3NWC3wu64EH2byu4VCdGnfYIiTOuYKMnQLl6cO9fjr9a+x6YICD0XB4IiYmnPFNYatJx7Am94xywJ1oGM+I/Vki/UT2ixgvM2NVh1AAJ/lHpWbcw=="
          }
        }"""


      val body = JsonParser(bodyJson).convertTo[WebEngageSMSBody]

      for {
        status <- SmsService.sendSMS(List(body.smsData.toNumber), body.smsData.body)
      } yield {
        assert(status == StatusCodes.OK)
      }

    }

  }

}