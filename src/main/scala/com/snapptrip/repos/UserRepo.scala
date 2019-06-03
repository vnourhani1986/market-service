package com.snapptrip.repos

import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.DI._
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.models.User
import com.snapptrip.utils.DateTimeUtils
import com.snapptrip.utils.PostgresProfiler.api._
import slick.lifted.ProvenShape

import scala.concurrent.Future

trait WebEngageUserRepo {

  def findByUserName(userName: Option[String]): Future[Option[User]]

  def findOptionalByUserName(userName: Option[String]): Future[Option[User]]

  def findOrCreateByUserName(userName: Option[String]): Future[User]

  def get: Future[Seq[User]]

  def findByFilter(filter: WebEngageUserInfo): Future[Option[User]]

  def findByFilter(mobileNo: Option[String], email: Option[String]): Future[Option[User]]

  def save(user: User): Future[User]

  def update(user: User): Future[Boolean]

  def isDisabled(userName: String): Future[Boolean]

}

object WebEngageUserRepoImpl extends WebEngageUserRepo with WebEngageUserTableComponent {
  override def findByUserName(userName: Option[String]): Future[Option[User]] = {
    val query = userTable.filterOpt(userName)((table, un) => table.userName === un).result.headOption
    db.run(query)
  }

  override def findOptionalByUserName(userName: Option[String]): Future[Option[User]] = {
    val query = userTable.filterOpt(userName)((table, un) => table.userName === un).result.headOption
    db.run(query)
  }

  override def findOrCreateByUserName(userName: Option[String]): Future[User] = {

    val queryToFind = userTable.filterOpt(userName)((table, un) => table.userName === un).result.headOption
    val queryToInsert = userTable returning userTable += User(userName = userName, userId = "1")

    for {
      userOpt <- db.run(queryToFind)
      foundOrCreatedUser <- if (userOpt.isEmpty) db.run(queryToInsert) else Future {
        userOpt.get
      }
    } yield foundOrCreatedUser
  }

  override def get: Future[Seq[User]] = {
    val query = userTable
    db.run(query.result)
  }

  override def findByFilter(filter: WebEngageUserInfo): Future[Option[User]] = {

    val query = userTable
      //      .filterOpt(filter.user_name)((table, userName) => table.userName === userName)
      //      .filterOpt(filter.name)((table, name) => table.name === name)
      //      .filterOpt(filter.family)((table, family) => table.family === family)
      .filterOpt(filter.email)((table, email) => table.email === email || (table.email.isEmpty && filter.mobile_no.isDefined))
      .filterOpt(filter.mobile_no)((table, mobileNo) => table.mobileNo === mobileNo || (table.mobileNo.isEmpty && filter.email.isDefined))
      //      .filterOpt(filter.birth_date)((table, birthDate) => table.birthDate === birthDate)
      //      .filterOpt(filter.gender)((table, gender) => table.gender === gender)
      .result
      .headOption

    db.run(query)

  }

  override def findByFilter(mobileNo: Option[String], email: Option[String]): Future[Option[User]] = {

    val query = userTable
      //      .filterOpt(filter.user_name)((table, userName) => table.userName === userName)
      //      .filterOpt(filter.name)((table, name) => table.name === name)
      //      .filterOpt(filter.family)((table, family) => table.family === family)
      .filterOpt(email)((table, email) => table.email === email || (table.email.isEmpty && mobileNo.isDefined))
      .filterOpt(mobileNo)((table, mobileNo) => table.mobileNo === mobileNo || (table.mobileNo.isEmpty && email.isDefined))
      //      .filterOpt(filter.birth_date)((table, birthDate) => table.birthDate === birthDate)
      //      .filterOpt(filter.gender)((table, gender) => table.gender === gender)
      .result
      .headOption

    db.run(query)

  }

  override def save(user: User): Future[User] = {

    val action = userTable returning userTable.map(_.id) into ((table, id) =>
      table.copy(id = Some(id))) += User(None, user.userName, user.userId,
      DateTimeUtils.nowOpt, None, user.name, user.family, user.email, user.mobileNo, user.birthDate, user.gender, user.provider, disabled = user.disabled)
    db.run(action)

  }

  override def update(user: User): Future[Boolean] = {

    val action = userTable
      .filterOpt(user.email)((table, email) => table.email === email || (table.email.isEmpty && user.mobileNo.isDefined))
      .filterOpt(user.mobileNo)((table, mobileNo) => table.mobileNo === mobileNo || (table.mobileNo.isEmpty && user.email.isDefined))
      .map(x => (x.userName, x.name, x.family, x.email, x.mobileNo, x.birthDate, x.gender, x.modifiedAt, x.disabled))
      .update((user.userName, user.name, user.family, user.email, user.mobileNo, user.birthDate, user.gender, DateTimeUtils.nowOpt,
        user.disabled))

    db.run(action).map(_ > 0)

  }

  override def isDisabled(userName: String): Future[Boolean] = {

    val query = userTable
      .filter(_.userName === userName)
      .map(_.disabled)

    db.run(query.result.head)

  }

}

private[repos] trait WebEngageUserTableComponent extends SlickSupport {

 private[WebEngageUserTableComponent] final class UserTable(tag: Tag)
    extends Table[User](tag, "user") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def userId: Rep[String] = column[String]("user_id")

    def userName: Rep[Option[String]] = column[Option[String]]("user_name")

    def createdAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("created_at")

    def modifiedAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("modified_at")

    def name: Rep[Option[String]] = column[Option[String]]("name")

    def family: Rep[Option[String]] = column[Option[String]]("family")

    def email: Rep[Option[String]] = column[Option[String]]("email")

    def mobileNo: Rep[Option[String]] = column[Option[String]]("mobile_no")

    def birthDate: Rep[Option[LocalDate]] = column[Option[LocalDate]]("birth_date")

    def gender: Rep[Option[String]] = column[Option[String]]("gender")

    def provider: Rep[Option[String]] = column[Option[String]]("provider")

    def disabled: Rep[Boolean] = column[Boolean]("disabled", O.Default(false))

    def deleted: Rep[Boolean] = column[Boolean]("deleted", O.Default(false))

    def * : ProvenShape[User] = (
      id.?,
      userName,
      userId,
      createdAt,
      modifiedAt,
      name,
      family,
      email,
      mobileNo,
      birthDate,
      gender,
      provider,
      disabled,
      deleted) <> ((User.apply _).tupled, User.unapply)
  }

  protected val userTable = TableQuery[UserTable]

}