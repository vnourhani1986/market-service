package com.snapptrip.utils

import com.snapptrip.DI._
import com.snapptrip.api.Messages.{OpengdprRequests, SubjectIdentities}
import com.snapptrip.formats.Formats._
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.snapptrip.services.WebEngage
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object DeleteWebEngageUsersUtil {

  def deleteWebEngageUsersTable(): Unit = {

    println("delete webengage users started ...")
    WebEngageUserRepoImpl.deletedUsersFinalGet.map { userIds =>
      userIds.foreach { userId =>
        println(userId)
        val result = for {
          ors <- WebEngage.opengdprRequests(OpengdprRequests(
            userId,
            "erasure",
            List(SubjectIdentities(
              "cuid",
              userId
            ))).toJson)
          orsgs <- WebEngage.opengdprRequestsGetStatus(userId)
          disabled <- WebEngageUserRepoImpl.deletedUsersFinalUpdate(userId, true, false)
          //    orsd <- WebEngage.opengdprRequestsDelete("77670bbf-6e7b-42bb-201d-f38d2b8a5a8b46251")
        } yield {
          println(ors)
          println(orsgs)
          //    println(orsd)
//          println(disabled)
        }
        Await.result(result, Duration.Inf)
      }

    }
  }

}
//
//object runner extends App {
//  DeleteWebEngageUsersUtil.deleteWebEngageUsersTable()
//}
