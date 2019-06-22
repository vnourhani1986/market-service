package com.snapptrip.repos

import com.snapptrip.models.User
import com.typesafe.scalalogging.LazyLogging
import org.scalamock.matchers.Matchers
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec

import scala.concurrent.Future

class UserRepoTest extends AsyncFlatSpec with AsyncMockFactory with Matchers with LazyLogging {

  val user = User(userId = "1234567890")

  "user repository" should "query users as" in {

//    val userRepo: WebEngageUserRepo = stub[WebEngageUserRepo]
//    //    (userRepo.findByFilter() _).when(*).returns(Future.successful(Some(user)))
//    (userRepo.findByFilter(_: Option[String] , _: Option[String])).when(*, *).returns(Future.successful(Some(user)))

    val result = for {
      user1 <- WebEngageUserRepoImpl.findByFilter(Some("9124497401"), None)
      user2 <- WebEngageUserRepoImpl.findByUserName(Some("3213"))
    } yield (user1, user2)

    result.map{x =>
      assert(x._1.isDefined && x._2.isEmpty)}
  }

}
