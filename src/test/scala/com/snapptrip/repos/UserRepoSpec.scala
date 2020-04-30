package com.snapptrip.repos

import com.snapptrip.api.Messages.WebEngageUserInfo
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

  "user repository" should "query users with these criteria: " +
    "1) when we query user with email and have one user with only the email should find the user " +
    "2) when we query user with mobile no and have one user with only the mobile no should find the user " +
    "3) when we query user with email and mobile no and have one user with only the email and have the other with only the mobile no should show the newest one " +
    "4) when we query user with email and/or mobile no or vise versa and have user with only one of them and have the other with both of them should show the user with both of them " in {

    val userId1 = "1234567891"
    val userId2 = "1234567892"
    val userId3 = "1234567893"
    val email = "test1@test.ir"
    val mobileNo = "9121111113"

    val result = for {
      // phase 1
      sUser1 <- UserRepoImpl.save(User(userId = userId1, email = Some(email))) // save new user by email
      fUser11 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo))) // find by mobile no
      _ = logger.info(s"""phase11->${fUser11.toString}""")
      fUser12 <- UserRepoImpl.find(WebEngageUserInfo(email = Some(email))) // find by email
      _ = logger.info(s"""phase12->${fUser12.toString}""")
      fUser13 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo), email = Some(email))) // find by mobile and email
      _ = logger.info(s"""phase13->${fUser13.toString}""")
      // phase 2
      _ <- UserRepoImpl.save(User(userId = userId2, mobileNo = Some(mobileNo))) // save new user by mobile no
      fUser21 <- UserRepoImpl.find(WebEngageUserInfo(email = Some(email))) // find by email
      _ = logger.info(s"""phase21->${fUser21.toString}""")
      fUser22 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo))) // find by mobile no
      _ = logger.info(s"""phase22->${fUser22.toString}""")
      fUser23 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo), email = Some(email))) // find by mobile and email
      _ = logger.info(s"""phase23->${fUser23.toString}""")
      // phase 3
      _ <- UserRepoImpl.update(sUser1.copy(mobileNo = Some(mobileNo))) // update user1 by mobile no
      fUser31 <- UserRepoImpl.find(WebEngageUserInfo(email = Some(email))) // find by email
      _ = logger.info(s"""phase31->${fUser31.toString}""")
      fUser32 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo))) // find by mobile no
      _ = logger.info(s"""phase32->${fUser32.toString}""")
      fUser33 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo), email = Some(email))) // find by email and mobile
      _ = logger.info(s"""phase33->${fUser33.toString}""")
      // phase 4
      _ <- UserRepoImpl.save(User(userId = userId3, email = Some(email))) // save new user by email
      fUser41 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo))) // find by mobile no
      _ = logger.info(s"""phase41->${fUser41.toString}""")
      fUser42 <- UserRepoImpl.find(WebEngageUserInfo(email = Some(email))) // find by email
      _ = logger.info(s"""phase42->${fUser42.toString}""")
      fUser43 <- UserRepoImpl.find(WebEngageUserInfo(mobile_no = Some(mobileNo), email = Some(email))) // find by mobile and email
      _ = logger.info(s"""phase43->${fUser43.toString}""")
      // delete users
      _ <- UserRepoImpl.delete(userId1)
      _ <- UserRepoImpl.delete(userId2)
      _ <- UserRepoImpl.delete(userId3)
    } yield (
      fUser11,
      fUser12,
      fUser13,
      fUser21,
      fUser22,
      fUser23,
      fUser31,
      fUser32,
      fUser33,
      fUser41,
      fUser42,
      fUser43
    )

    // phase 1 (we have a new user with only email)
    result.map(_._1.isDefined).map(r11 => assert(!r11))
    result.map(_._2.get).map(r12 => assert(r12.userId == userId1))
    result.map(_._3.get).map(r13 => assert(r13.userId == userId1))

    // phase 2 (we have a new user with only mobileNo)
    result.map(_._4.get).map(r21 => assert(r21.userId == userId1))
    result.map(_._5.get).map(r22 => assert(r22.userId == userId2))
    result.map(_._6.get).map(r23 => assert(r23.userId == userId2)) // the newer one

    // phase 3 (we have a user with mobileNo, email and we have user with mobileNo)
    result.map(_._7.get).map(r31 => assert(r31.userId == userId1)) // completest one
    result.map(_._8.get).map(r32 => assert(r32.userId == userId1))
    result.map(_._9.get).map(r33 => assert(r33.userId == userId1))

    // phase 4 (we have a user with mobileNo, email and we have user with mobileNo and have user with email)
    result.map(_._10.get).map(r41 => assert(r41.userId == userId1)) // completest one
    result.map(_._11.get).map(r42 => assert(r42.userId == userId1))
    result.map(_._12.get).map(r43 => assert(r43.userId == userId1))

  }

}
