package com.snapptrip.models

import java.time.LocalDateTime

import com.snapptrip.auth.UserRole
import com.snapptrip.utils.DateTimeUtils

case class User(
                 id: Option[Long],
                 userName: String,
                 businessId: Option[Long] = None,
                 loginToken: Option[String] = None,
                 role: String = UserRole.GUEST,
                 createdAt: Option[LocalDateTime] = DateTimeUtils.nowOpt,
                 modifiedAt: Option[LocalDateTime] = DateTimeUtils.nowOpt,
                 name: Option[String] = None,
                 family: Option[String] = None,
                 email: Option[String] = None,
                 disabled: Boolean = false,
                 deleted: Boolean = false
               )
