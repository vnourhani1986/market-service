package com.snapptrip.models

import java.time.LocalDateTime

import com.snapptrip.api.Messages.BusinessName

case class Business(
                     id: Long,
                     name: BusinessName,
                     code: String,
                     phone: String,
                     email: String,
                     password: String,
                     accessToken: String,
                     publicKey: String,
                     createdAt: LocalDateTime,
                     modifiedAt: LocalDateTime,
                     enabled: Boolean = true,
                     deleted: Boolean = false
                   )




