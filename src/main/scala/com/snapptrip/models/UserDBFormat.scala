package com.snapptrip.models

case class UserDBFormat(
                         id: Option[Long] = None,
                         user_name: Option[String] = None,
                         user_id: String,
                         created_at: Option[String] = None,
                         modified_at: Option[String] = None,
                         name: Option[String] = None,
                         family: Option[String] = None,
                         email: Option[String] = None,
                         origin_email: List[String] = Nil,
                         mobile_no: Option[String] = None,
                         birth_date: Option[String] = None,
                         gender: Option[String] = None,
                         provider: Option[String] = None,
                         disabled: Boolean = false,
                         deleted: Boolean = false
                       )
