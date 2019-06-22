package com.snapptrip.notification

import akka.http.scaladsl.model.StatusCodes
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.snapptrip.DI.{ec, system}
import com.snapptrip.api.Messages.WebEngageEmailBody
import com.snapptrip.formats.Formats._
import com.snapptrip.notification.email.EmailService
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.JsonParser

import scala.concurrent.duration._

class EmailServiceTest extends TestKit(system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val timeout: Timeout = Timeout(1.minute)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An webengage actor" must {

    "send back messages :" in {


      val bodyJson =
        s"""{
          "email": {
              "from": "from@email.xyz",
              "fromName": "John Doe",
              "replyTo": [
                  "vahid.nourhani@snapptrip.com",
                  "replytwo@email.xyz"
              ],
              "subject": "email subject",
              "text": "text body",
              "html": "html body",
              "recipients": {
                  "to": [{
                      "name": "Recipient1",
                      "email": "vahid.nourhani@snapptrip.com"
                  }, {
                      "name": "Recipient2",
                      "email": "vahid.nourhani@snapptrip.com"
                  }],
                  "cc": [
                      "recipient_cc1@email.xyz",
                      "recipient_cc2@email.xyz"
                  ],
                  "bcc": [
                      "recipient_bcc1@email.xyz",
                      "recipient_bcc2@email.xyz"
                  ]
              },
              "attachments": [{
                  "name": "Attachment1",
                  "url": "http://link/to/attachment/1"
              },{
                  "name": "Attachment2",
                  "url": "http://link/to/attachment/2"
              }]
          },
          "metadata": {
              "campaignType": "PROMOTIONAL",
              "timestamp": "2018-01-25T10:24:16+0000",
              "messageId": "webengage-message-id"
          },
          "version": "1.0"
        }"""


      val body = JsonParser(bodyJson).convertTo[WebEngageEmailBody]

      for {
        status <- EmailService.sendEmail(body.email.subject,
          body.email.text,
          body.email.recipients.to.head.email,
          body.email.fromName,
          None)
      } yield {
        assert(status == StatusCodes.OK)
      }

    }

  }

}