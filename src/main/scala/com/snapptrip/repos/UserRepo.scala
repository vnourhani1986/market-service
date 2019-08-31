package com.snapptrip.repos

import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.DI._
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.models.User
import com.snapptrip.utils.DateTimeUtils
import com.snapptrip.utils.PostgresProfiler.api._
import com.snapptrip.utils.formatters.EmailFormatter
import slick.lifted.ProvenShape

import scala.concurrent.Future

trait WebEngageUserRepo {

  def findByUserName(userName: Option[String]): Future[Option[User]]

  def findOptionalByUserName(userName: Option[String]): Future[Option[User]]

  def findOrCreateByUserName(userName: Option[String]): Future[User]

  def get: Future[Seq[User]]

  def get(take: Int): Future[Seq[User]]

  def deletedUsersFinalGet: Future[Seq[String]]

  def deletedUsersFinalUpdate(userId: String, disabled: Boolean, deleted: Boolean): Future[Int]

  def findByFilter(filter: WebEngageUserInfo): Future[Option[User]]

  def findByFilter(mobileNo: Option[String], email: Option[String]): Future[Option[User]]

  def find(mobileNo: Option[String], email: Option[String]): Future[Seq[User]]

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
      .filter(_.disabled === false)
      .filter(_.deleted === false)
    db.run(query.result)
  }

  override def get(take: Int): Future[Seq[User]] = {
    val query = userTable
      .filter(_.disabled === false)
      .filter(_.deleted === false)
      .take(take)
    db.run(query.result)
  }

  override def deletedUsersFinalGet: Future[Seq[String]] = {
    val query = sql"""SELECT user_id from delete_users_final where disabled = false and deleted = false limit 10;"""
      .as[String]
    db.run(query)
  }

  override def deletedUsersFinalUpdate(userId: String, disabled: Boolean, deleted: Boolean): Future[Int] = {
    val query = sql"""update delete_users_final SET (disabled, deleted) = ($disabled, $deleted) where user_id = $userId;"""
      .as[Int]
    db.run(query).map(_.head)
  }

  override def findByFilter(filter: WebEngageUserInfo): Future[Option[User]] = {

    val em = EmailFormatter.format(filter.email)

    val query = ((filter.mobile_no, em) match {
      case (Some(m), Some(e)) => userTable
        .filter(table => table.mobileNo === m && table.email === e)
      case (Some(m), None) => userTable
        .filter(table => table.mobileNo === m)
      case (None, Some(e)) => userTable
        .filter(table => table.email === e)
      case (None, None) => userTable
    }).sortBy(table => (table.mobileNo, table.email)).result.headOption

    db.run(query)

  }

  override def findByFilter(mobileNo: Option[String], email: Option[String]): Future[Option[User]] = {

    val em = EmailFormatter.format(email)

    val query = ((mobileNo, em) match {
      case (Some(m), Some(e)) => userTable
        .filter(table => table.mobileNo === m && table.email === e)
      case (Some(m), None) => userTable
        .filter(table => table.mobileNo === m)
      case (None, Some(e)) => userTable
        .filter(table => table.email === e)
      case (None, None) => userTable
    }).sortBy(table => (table.mobileNo, table.email)).result.headOption

    db.run(query)

  }

  override def find(mobileNo: Option[String], email: Option[String]): Future[Seq[User]] = {

    val em = EmailFormatter.format(email)

    val query = ((mobileNo, em) match {
      case (Some(m), Some(e)) => userTable
        .filter(table => table.mobileNo === m && table.email === e)
      case (Some(m), None) => userTable
        .filter(table => table.mobileNo === m)
      case (None, Some(e)) => userTable
        .filter(table => table.email === e)
      case (None, None) => userTable
    }).sortBy(table => (table.mobileNo, table.email)).result

    db.run(query)

  }

  override def save(user: User): Future[User] = {

    val action = userTable returning userTable.map(_.id) into ((table, id) =>
      table.copy(id = Some(id))) += User(None, user.userName, user.userId,
      DateTimeUtils.nowOpt, None, user.name, user.family, user.email, user.originEmail, user.mobileNo, user.birthDate, user.gender, user.provider, disabled = user.disabled)
    db.run(action)

  }

  override def update(user: User): Future[Boolean] = {

    val action = ((user.mobileNo, user.email) match {
      case (Some(m), Some(e)) => userTable
        .filter(table => table.mobileNo === m && table.email === e)
      case (Some(m), None) => userTable
        .filter(table => table.mobileNo === m)
      case (None, Some(e)) => userTable
        .filter(table => table.email === e)
      case (None, None) => userTable
    }).sortBy(table => (table.mobileNo, table.email))
      .take(1)
      .map(x => (x.userName, x.name, x.family, x.email, x.originEmail, x.mobileNo, x.birthDate, x.gender, x.modifiedAt, x.disabled, x.provider))
      .update((user.userName, user.name, user.family, user.email, user.originEmail, user.mobileNo, user.birthDate, user.gender, DateTimeUtils.nowOpt,
        user.disabled, user.provider))

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

    def originEmail: Rep[List[String]] = column[List[String]]("origin_email")

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
      originEmail,
      mobileNo,
      birthDate,
      gender,
      provider,
      disabled,
      deleted) <> ((User.apply _).tupled, User.unapply)
  }

  protected val userTable = TableQuery[UserTable]

}
