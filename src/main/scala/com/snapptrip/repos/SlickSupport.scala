package com.snapptrip.repos

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.formats.Formats._
import com.snapptrip.utils.PostgresProfiler.api._

trait SlickSupport {
  // Slick mapper (converts Airline to String and vice versa)

  implicit val timestamplocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    { localDateTime => Timestamp.valueOf(localDateTime) },
    { timeStamp => timeStamp.toLocalDateTime }
  )

  implicit val dateMapper: BaseColumnType[Date] = MappedColumnType.base[Date, String](_.toString, Date.valueOf)
  implicit val localDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](_.toString, LocalDate.parse(_, shortDateFormatter))

}
