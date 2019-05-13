package com.snapptrip.repos.database

import com.snapptrip.DI._
import com.snapptrip.repos.UsersTableComponent
import com.snapptrip.utils.PostgresProfiler.api._
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.meta.MTable

import scala.concurrent.Future


object DBSetup extends UsersTableComponent with LazyLogging {


  private val tables = List(
    //    compensationTable,
    //    bookingDescriptionTable,
    //    bookingRefundInfoTable,
        usersTable,
    //    campaignTable,
    //    refundResultsTable
  )

  def initDbs(): Future[List[Unit]] = {
    logger.info(s"initiating dbs on : ${config.getString("db.url")}")
    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist =
        tables.filter(table => !names.contains(table.baseTableRow.tableName))
          .map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    f
  }
}

