package com.snapptrip.utils

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate, LocalDateTime, ZoneId}
import com.snapptrip.DI._


object DateTimeUtils {

  implicit val timezone: ZoneId = ZoneId.of(config.getString("timezone"))

  def now: LocalDateTime = LocalDateTime.now(timezone)

  def nowOpt: Option[LocalDateTime] = Option(now)

  def nowDate: LocalDate = LocalDate.now(timezone)

  val firstDate: LocalDate = LocalDate.parse("2017-12-16", DateTimeFormatter.ISO_DATE)

  /**
    * Returns remaining time
    *
    * @param requestedAt
    * @param expiresAt
    * @return
    */
  def remainingInSeconds(requestedAt: LocalDateTime, expiresAt: LocalDateTime): Long = {
    if (now.isBefore(expiresAt))
      Math.max(Duration.between(now, expiresAt).toMillis, 0l) / 1000
    else
      0l
  }
}
