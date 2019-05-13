package com.snapptrip.repos

import java.time.LocalDateTime

import com.snapptrip.DI._
import com.snapptrip.api.Messages.{BusinessInfo, BusinessName}
import com.snapptrip.models.{Business, _}
import com.snapptrip.utils.PostgresProfiler.api._
import slick.lifted.{ProvenShape, Rep}
import spray.json.JsonParser

import scala.concurrent.Future

trait BusinessRepo {
  def findById(id: Long): Future[Option[Business]]

  def findByCode(code: String): Future[Option[Business]]

  def findByAccessToken(accessToken: String): Future[Option[Business]]

  def findByBusinessId(businessId: Option[Long]): Future[Option[String]]

  def get: Future[Seq[BusinessInfo]]

}

object BusinessRepoImpl extends BusinessRepo with BusinessTableComponent {
  override def findById(id: Long): Future[Option[Business]] = {
    val query = businessTable.filter(_.id === id).result.headOption
    db.run(query)
  }

  override def findByCode(code: String): Future[Option[Business]] = {
    val query = businessTable.filter(_.code === code).result.headOption
    db.run(query)
  }

  override def findByAccessToken(accessToken: String): Future[Option[Business]] = {
    val query = businessTable.filter(_.accessToken === accessToken).result.headOption
    db.run(query)
  }

  override def findByBusinessId(businessId: Option[Long]): Future[Option[String]] = {
    val query = businessTable.filter(_.id === businessId).result.headOption
    db.run(query).map(_.map(_.code))

  }

  override def get: Future[Seq[BusinessInfo]] = {
    val query = businessTable
    db.run(query.result).map(_.map(x => BusinessInfo(x.id, Some(x.name),
      Some(x.code), Some(x.phone), Some(x.email))))
  }

}


private[repos] trait BusinessTableComponent extends SlickSupport {

  final class BusinessTable(tag: Tag) extends Table[Business](tag, "businesses") {

    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name: Rep[BusinessName] = column[BusinessName]("name")

    def code: Rep[String] = column[String]("code", O.Unique)

    def phone: Rep[String] = column[String]("phone")

    def email: Rep[String] = column[String]("email")

    def password: Rep[String] = column[String]("password")

    def accessToken: Rep[String] = column[String]("access_token", O.Unique)

    def publicKey: Rep[String] = column[String]("public_key")

    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

    def modifiedAt: Rep[LocalDateTime] = column[LocalDateTime]("modified_at")

    def enabled: Rep[Boolean] = column[Boolean]("enabled")

    def deleted: Rep[Boolean] = column[Boolean]("deleted")


    def * : ProvenShape[Business] = (
      id,
      name,
      code,
      phone,
      email,
      password,
      accessToken,
      publicKey,
      createdAt,
      modifiedAt,
      enabled,
      deleted
    ) <> ((Business.apply _).tupled, Business.unapply)
  }

  protected val businessTable = TableQuery[BusinessTable]

}