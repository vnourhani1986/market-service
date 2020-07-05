package com.snapptrip.repos

import scala.concurrent.Future

trait Repo[A, Filter] {

  def find(filter: Filter): Future[Option[A]]

  def find(filter: String): Future[Option[A]]

  def save(user: A): Future[A]

  def update(user: A): Future[Boolean]

}
