package com.snapptrip.webengage

import akka.http.scaladsl.model.StatusCodes
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.snapptrip.DI.{ec, system}
import com.snapptrip.api.Messages.WebEngageEvent
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.JsonParser
import com.snapptrip.formats.Formats._
import com.snapptrip.webengage.api.WebEngageApi

import scala.concurrent.duration._

class WebEngageApiTest extends TestKit(system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val timeout: Timeout = Timeout(1.minute)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An webengage actor" must {

    "send back messages :" in {


      val user =
        s"""{
	        "userId": "9124497404",
          "firstName": "vahid",
          "lastName": "nourhani",
          "birthDate": "1986-08-19T15:45:00-0800",
          "gender":"male",
          "email":"vnourhani1986@gmail.com",
          "phone":"+551155256325",
          "company":"Alphabet Inc.",
          "attributes": {
          	"Age":"31",
            "Twitter username": "@origjohndoe86",
            "Dollars spent": 461.93,
            "Points earned": 78732
            }
          }"""

      val event =
        s"""{
	        "user": {
	        		"email": "test@pintapin.com",
	        		"mobile_no": "9124497411"
	        	},
	        "event": {
	        		"eventName": "Fulfilled",
	        		"eventTime": "2018-09-15T18:29:00-0800",
	        		"eventData": {
	        			"cityName": "Tehran"
	        	}

            }
        }"""

      for {
        userResponse <- WebEngageApi.trackUser(JsonParser(user))
        eventResponse <- WebEngageApi.trackEventWithoutUserId(JsonParser(event).convertTo[WebEngageEvent])
      } yield {
        assert(userResponse._1 == StatusCodes.OK && eventResponse._1)
      }

    }

  }

}