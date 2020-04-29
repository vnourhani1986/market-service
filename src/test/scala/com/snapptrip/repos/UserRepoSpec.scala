package com.snapptrip.repos

import com.snapptrip.models.User
import com.typesafe.scalalogging.LazyLogging
import org.scalamock.matchers.Matchers
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.AsyncFlatSpec

class UserRepoSpec
  extends AsyncFlatSpec
    with AsyncMockFactory
    with Matchers
    with LazyLogging {

  val user = User(userId = "1234567890")

  "user repository" should "query users as" in {

    val result = for {
      user1 <- WebEngageUserRepoImpl.findByFilter(Some("9122683744"), None)
      user2 <- WebEngageUserRepoImpl.findByFilter(None, Some("siavash.rahmani@snapptrip.com"))
      user3 <- WebEngageUserRepoImpl.findByFilter(Some("9122683744"), Some("siavash.rahmani@snapptrip.com"))
    } yield (user1, user2, user3)

    result.map { x =>
      assert(x._1.isDefined && x._2.isEmpty)
    }
  }

}
