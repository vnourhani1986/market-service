package com.snapptrip.utils

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.json.PgJsonExtensions



trait PostgresProfiler extends ExPostgresProfile
  with PgArraySupport
  with PgRangeSupport
  with PgDateSupport
  with PgDate2Support
  with PgSearchSupport
  with PgSprayJsonSupport
  with PgEnumSupport{


  override val pgjson = "jsonb"

  override val api: API = new API{}

  trait API extends super.API
    with ArrayImplicits
    with DateTimeImplicits
    with SimpleDateTimeImplicits
    with SearchImplicits
    with SearchAssistants
    with JsonImplicits

//    with Date2DateTimeImplicitsDuration{}

}

object PostgresProfiler extends PostgresProfiler with PgJsonExtensions

