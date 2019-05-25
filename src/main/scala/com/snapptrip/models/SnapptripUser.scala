package com.snapptrip.models

import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.utils.DateTimeUtils

case class SnapptripUser(
                          id: Option[Long] = None,
                          userName: Option[String] = None,
                          createdAt: Option[LocalDateTime] = DateTimeUtils.nowOpt,
                          modifiedAt: Option[LocalDateTime] = DateTimeUtils.nowOpt,
                          name: Option[String] = None,
                          family: Option[String] = None,
                          email: Option[String] = None,
                          mobileNo: Option[String] = None,
                          birthDate: Option[LocalDate] = None,
                          gender: Option[String] = None,
                          disabled: Boolean = false,
                          deleted: Boolean = false
                        )
