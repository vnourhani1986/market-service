package com.snapptrip.repos.database

import com.snapptrip.DI.config
import org.flywaydb.core.Flyway

object DBMigration {

  lazy val dbUrl: String = config.getString("db.url")
  lazy val dbUser: String = config.getString("db.user")
  lazy val dbPass: String = config.getString("db.password")

  private val flyway = new Flyway()
  flyway.setDataSource(dbUrl, dbUser, dbPass)
  flyway.setBaselineOnMigrate(true)

  def migrateDatabaseSchema(): Unit = flyway.migrate()

}
