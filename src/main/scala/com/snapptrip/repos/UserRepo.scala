package com.snapptrip.repos

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.snapptrip.DI._
import com.snapptrip.api.Messages.WebEngageUserInfo
import com.snapptrip.formats.Formats._
import com.snapptrip.models.{User, UserDBFormat}
import com.snapptrip.utils.PostgresProfiler.api._
import com.snapptrip.utils.formatters.EmailFormatter
import slick.lifted.ProvenShape
import spray.json.JsonParser

import scala.concurrent.Future

trait UserRepo extends Repo[User, WebEngageUserInfo] {

  def find(filter: WebEngageUserInfo): Future[Option[User]]

  def find(mobileNo: Option[String], email: Option[String]): Future[Seq[User]]

  def save(user: User): Future[User]

  def update(user: User): Future[Boolean]

  def delete(userId: String): Future[Boolean]

}

object UserRepoImpl extends UserRepo with UserTableComponent {

  override def find(filter: WebEngageUserInfo): Future[Option[User]] = {

    val em = EmailFormatter.format(filter.email)

    val e = em.map(x => s"""'$x'""").getOrElse(s"""null""")
    val m = filter.mobile_no.map(x => s"""'$x'""").getOrElse(s"""null""")
    val query = sql"""SELECT * from ptp_fn_find_user(#$e, #$m);"""
      .as[Option[String]]
    db.run(query).map(_.map(_.map(r => get(r))).headOption.flatten)

  }

  override def find(mobileNo: Option[String], email: Option[String]): Future[Seq[User]] = {

    val em = EmailFormatter.format(email)

    val e = em.map(x => s"""'$x'""").getOrElse(s"""null""")
    val m = mobileNo.map(x => s"""'$x'""").getOrElse(s"""null""")
    val query = sql"""SELECT * from ptp_fn_find_user(#$e, #$m);"""
      .as[String]
    db.run(query).map(res => {
      res.map { r => get(r) }
    })

  }

  override def save(user: User): Future[User] = {

    val action = userTable returning userTable.map(_.id) into ((table, id) =>
      table.copy(id = Some(id))) += user
    db.run(action)

  }

  override def update(user: User): Future[Boolean] = {

    val action = userTable
      .filter(_.id === user.id)
      .update(user)
    db.run(action).map(_ > 0)

  }

  def delete(userId: String): Future[Boolean] = {

    val action = userTable
      .filter(_.userId === userId)
      .delete
    db.run(action).map(_ == 1)

  }

  def get(result: String): User = {

    val u = JsonParser(result).convertTo[UserDBFormat]
    User(u.id, u.user_name, u.user_id,
      u.created_at.map(x => LocalDateTime.parse(x, DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
      u.modified_at.map(x => LocalDateTime.parse(x, DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
      u.name, u.family, u.email, u.origin_email, u.mobile_no,
      u.birth_date.map(x => LocalDate.parse(x, DateTimeFormatter.ISO_LOCAL_DATE)),
      u.gender, u.provider, u.disabled, u.deleted
    )

  }

}

private[repos] trait UserTableComponent extends SlickSupport {

  private[UserTableComponent] final class UserTable(tag: Tag)
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