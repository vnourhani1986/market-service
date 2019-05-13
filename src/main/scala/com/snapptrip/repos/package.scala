package com.snapptrip

import slick.lifted.{CanBeQueryCondition, Query, Rep}

package object repos {

  implicit class ConditionalFilterQuery[E, U, C[_]](val q: Query[E, U, C]) extends AnyVal {
    def filterOpt[D, T <: Rep[_] : CanBeQueryCondition](opt: Option[D])(f: (E, D) => T): Query[E, U, C] =
      opt.map(d => q.filter(a => f(a, d))).getOrElse(q)
  }
}
