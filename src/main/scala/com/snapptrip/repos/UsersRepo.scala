package com.snapptrip.repos

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import com.snapptrip.DI._
import com.snapptrip.api.Messages.{EditUser, FilterUser, NewUser}
import com.snapptrip.auth.UserRole
import com.snapptrip.models.User
import com.snapptrip.utils.PostgresProfiler.api._
import com.snapptrip.utils.{DateTimeUtils, ErrorCodes, UserException}
import slick.lifted.ProvenShape

import scala.concurrent.Future

trait UsersRepo {

  def findByUserName(userName: String): Future[Option[User]]

  def findOptionalByUserName(userName: String): Future[Option[User]]

  def findOrCreateByUserName(userName: String): Future[User]

  def setRole(userName: String, role: String): Future[Unit]

  def get: Future[Seq[User]]

  def findByFilter(filter: FilterUser): Future[Seq[User]]

  def save(user: NewUser): Future[Long]

  def update(user: EditUser): Future[Boolean]

  def isDisabled(userName: String): Future[Boolean]

}

object UsersRepoImpl extends UsersRepo with UsersTableComponent {
  override def findByUserName(userName: String): Future[Option[User]] = {
    val query = usersTable.filter(_.userName === userName).result.headOption
    db.run(query)
  }

  override def findOptionalByUserName(userName: String): Future[Option[User]] = {
    val query = usersTable.filter(_.userName === userName).result.headOption
    db.run(query)
  }

  override def findOrCreateByUserName(userName: String): Future[User] = {

    val queryToFind = usersTable.filter(_.userName === userName).result.headOption
    val queryToInsert = usersTable returning usersTable += User(None, userName)

    for {
      userOpt <- db.run(queryToFind)
      foundOrCreatedUser <- if (userOpt.isEmpty) db.run(queryToInsert) else Future {
        userOpt.get
      }
    } yield foundOrCreatedUser
  }

  override def setRole(email: String, role: String): Future[Unit] = ???

  override def get: Future[Seq[User]] = {
    val query = usersTable
    db.run(query.result)
  }

  override def findByFilter(filter: FilterUser): Future[Seq[User]] = {

    val query = usersTable
      .filterOpt(filter.userName)((table, userName) => table.userName === userName)
      .filterOpt(filter.name)((table, name) => table.name.like(name))
      .filterOpt(filter.family)((table, family) => table.family.like(family))
      .filterOpt(filter.email)((table, email) => table.email.like(email))
      .filterOpt(filter.role)((table, role) => table.role === role)

    db.run(query.result)

  }

  override def save(user: NewUser): Future[Long] = {

    findByUserName(user.userName).flatMap {
      case Some(_) =>
        Future.failed(new UserException("user already exist", Some(ErrorCodes.USER_EXIST), StatusCodes.BadRequest))
      case None =>
        val action = usersTable returning usersTable.map(_.id) into ((table, id) =>
          table.copy(id = Some(id))) += User(None, user.userName, user.businessId, None,
          user.role, DateTimeUtils.nowOpt, None, user.name, user.family, user.email, disabled = user.disabled)
        db.run(action).map(_.id.get)
    }
  }

  override def update(user: EditUser): Future[Boolean] = {

    val action = usersTable
      .filter(table => table.userName === user.userName)
      .map(x => (x.userName, x.name, x.family, x.email, x.role, x.businessId, x.modifiedAt, x.disabled))
      .update((user.userName, user.name, user.family, user.email, user.role, user.businessId,
        DateTimeUtils.nowOpt, user.disabled))

    db.run(action).map(_ == 1)

  }

  override def isDisabled(userName: String): Future[Boolean] = {

    val query = usersTable
      .filter(_.userName === userName)
      .map(_.disabled)

    db.run(query.result.head)

  }

}

private[repos] trait UsersTableComponent extends SlickSupport {

  private[UsersTableComponent] final class UsersTable(tag: Tag)
    extends Table[User](tag, "users") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def userName: Rep[String] = column[String]("user_name")

    def businessId: Rep[Option[Long]] = column[Option[Long]]("business_id")

    def loginToken: Rep[Option[String]] = column[Option[String]]("login_token")

    def role: Rep[String] = column[String]("role", O.Default(UserRole.GUEST))

    def createdAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("created_at")

    def modifiedAt: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("modified_at")

    def name: Rep[Option[String]] = column[Option[String]]("name")

    def family: Rep[Option[String]] = column[Option[String]]("family")

    def email: Rep[Option[String]] = column[Option[String]]("email")

    def disabled: Rep[Boolean] = column[Boolean]("disabled", O.Default(false))

    def deleted: Rep[Boolean] = column[Boolean]("deleted", O.Default(false))

    def * : ProvenShape[User] = (
      id.?,
      userName,
      businessId,
      loginToken,
      role,
      createdAt,
      modifiedAt,
      name,
      family,
      email,
      disabled,
      deleted) <> ((User.apply _).tupled, User.unapply)
  }

  protected val usersTable = TableQuery[UsersTable]


}