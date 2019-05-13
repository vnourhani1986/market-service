package com.snapptrip.repos.database

import com.snapptrip.utils
import com.snapptrip.utils.PostgresProfiler
import com.typesafe.scalalogging.LazyLogging
import com.snapptrip.utils.PostgresProfiler.api._

object DBManager extends LazyLogging {

  lazy val db: utils.PostgresProfiler.backend.Database = connect

  private def connect: PostgresProfiler.backend.Database = {
    logger.info("connecting to db")
    Database.forConfig("db")
  }

  def closeDB(): Unit = {
    logger.info("closing db connections")
    db.close()
  }

}
