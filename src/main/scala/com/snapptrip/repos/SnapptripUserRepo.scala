package com.snapptrip.repos

import java.time.{LocalDate, LocalDateTime}

import akka.http.scaladsl.model.StatusCodes
import com.snapptrip.DI._
import com.snapptrip.api.Messages.{FilterUser, WebEngageUserInfo}
import com.snapptrip.models.SnapptripUser
import com.snapptrip.utils.PostgresProfiler.api._
import com.snapptrip.utils.{DateTimeUtils, ErrorCodes, UserException}
import slick.lifted.ProvenShape

import scala.concurrent.Future

trait SnapptripUserRepo {

  def findByUserName(userName: Option[String]): Future[Option[SnapptripUser]]

  def findOptionalByUserName(userName: Option[String]): Future[Option[SnapptripUser]]

  def findOrCreateByUserName(userName: Option[String]): Future[SnapptripUser]

  def get: Future[Seq[SnapptripUser]]

  def findByFilter(filter: WebEngageUserInfo): Future[Option[SnapptripUser]]

  def save(user: SnapptripUser): Future[Long]

  def update(user: SnapptripUser): Future[Boolean]

  def isDisabled(userName: String): Future[Boolean]

}

object SnapptripUserRepoImpl extends SnapptripUserRepo with SnapptripUserTableComponent {
  override def findByUserName(userName: Option[String]): Future[Option[SnapptripUser]] = {
    val query = snapptripUserTable.filterOpt(userName)((table, un) => table.userName === un).result.headOption
    db.run(query)
  }

  override def findOptionalByUserName(userName: Option[String]): Future[Option[SnapptripUser]] = {
    val query = snapptripUserTable.filterOpt(userName)((table, un) => table.userName === un).result.headOption
    db.run(query)
  }

  override def findOrCreateByUserName(userName: Option[String]): Future[SnapptripUser] = {

    val queryToFind = snapptripUserTable.filterOpt(userName)((table, un) => table.userName === un).result.headOption
    val queryToInsert = snapptripUserTable returning snapptripUserTable += SnapptripUser(userName = userName)

    for {
      userOpt <- db.run(queryToFind)
      foundOrCreatedUser <- if (userOpt.isEmpty) db.run(queryToInsert) else Future {
        userOpt.get
      }
    } yield foundOrCreatedUser
  }

  override def get: Future[Seq[SnapptripUser]] = {
    val query = snapptripUserTable
    db.run(query.result)
  }

  override def findByFilter(filter: WebEngageUserInfo): Future[Option[SnapptripUser]] = {

    val query = snapptripUserTable
      .filterOpt(filter.user_name)((table, userName) => table.userName === userName)
//      .filterOpt(filter.name)((table, name) => table.name === name)
//      .filterOpt(filter.family)((table, family) => table.family === family)
      .filterOpt(filter.email)((table, email) => table.email === email)
      .filterOpt(filter.mobile_no)((table, mobileNo) => table.mobileNo === mobileNo)
//      .filterOpt(filter.birth_date)((table, birthDate) => table.birthDate === birthDate)
//      .filterOpt(filter.gender)((table, gender) => table.gender === gender)
      .result
      .headOption

    db.run(query)

  }


  override def save(user: SnapptripUser): Future[Long] = {

    findByUserName(user.userName).flatMap {
      case Some(_) =>
        Future.failed(new UserException("user already exist", Some(ErrorCodes.USER_EXIST), StatusCodes.BadRequest))
      case None =>
        val action = snapptripUserTable returning snapptripUserTable.map(_.id) into ((table, id) =>
          table.copy(id = Some(id))) += SnapptripUser(None, user.userName,
          DateTimeUtils.nowOpt, None, user.name, user.family, user.email, user.mobileNo, user.birthDate, user.gender, disabled = user.disabled)
        db.run(action).map(_.id.get)
    }
  }

  override def update(user: SnapptripUser): Future[Boolean] = {

    val action = snapptripUserTable
      .filter(table => table.userName === user.userName)
      .map(x => (x.userName, x.name, x.family, x.email, x.modifiedAt, x.disabled))
      .update((user.userName, user.name, user.family, user.email, DateTimeUtils.nowOpt,
        user.disabled))

    db.run(action).map(_ == 1)

  }

  override def isDisabled(userName: String): Future[Boolean] = {

    val query = snapptripUserTable
      .filter(_.userName === userName)
      .map(_.disabled)

    db.run(query.result.head)

  }

}

private[repos] trait SnapptripUserTableComponent extends SlickSupport {

  private[SnapptripUserTableComponent] final class SnapptripUserTable(tag: Tag)
    extends Table[SnapptripUser](tag, "user_webengage") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def userName: Rep[Option[String]] = column[Option[String]]("user_name")

    def createdAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("created_at")

    def modifiedAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("modified_at")

    def name: Rep[Option[String]] = column[Option[String]]("name")

    def family: Rep[Option[String]] = column[Option[String]]("family")

    def email: Rep[Option[String]] = column[Option[String]]("email")

    def mobileNo: Rep[Option[String]] = column[Option[String]]("mobile_no")

    def birthDate: Rep[Option[LocalDate]] = column[Option[LocalDate]]("birth_date")

    def gender: Rep[Option[String]] = column[Option[String]]("gender")

    def disabled: Rep[Boolean] = column[Boolean]("disabled", O.Default(false))

    def deleted: Rep[Boolean] = column[Boolean]("deleted", O.Default(false))

    def * : ProvenShape[SnapptripUser] = (
      id.?,
      userName,
      createdAt,
      modifiedAt,
      name,
      family,
      email,
      mobileNo,
      birthDate,
      gender,
      disabled,
      deleted) <> ((SnapptripUser.apply _).tupled, SnapptripUser.unapply)
  }

  protected val snapptripUserTable = TableQuery[SnapptripUserTable]

}