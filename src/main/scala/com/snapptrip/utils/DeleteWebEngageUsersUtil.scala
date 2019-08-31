package com.snapptrip.utils

import com.snapptrip.DI._
import com.snapptrip.api.Messages.{OpengdprRequests, SubjectIdentities}
import com.snapptrip.formats.Formats._
import com.snapptrip.repos.WebEngageUserRepoImpl
import com.snapptrip.services.WebEngage
import spray.json._

import scala.concurrent.Future

object DeleteWebEngageUsersUtil {

  def deleteWebEngageUsersTable(): Future[Seq[Int]] = {

    println("delete webengage users started ...")
    for {
      userIds <- WebEngageUserRepoImpl.deletedUsersFinalGet
      seq <- Future.sequence(userIds.map { userId =>
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
          disabled
        }
        result
      })
    } yield {
      seq
    }
  }
}

//
//object runner extends App {
//  DeleteWebEngageUsersUtil.deleteWebEngageUsersTable()
//}
