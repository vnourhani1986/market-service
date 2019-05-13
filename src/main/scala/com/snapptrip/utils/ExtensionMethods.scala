package com.snapptrip.utils

import slick.ast.Ordering
import slick.ast.Ordering.Direction
import slick.lifted.{ColumnOrdered, Ordered, Query, Rep}

object ExtensionMethods {

  object DynamicSortBySupport {

    // To enable higher kind function and remove scala compiler warning
    import scala.language.higherKinds

    type ColumnOrdering = (String, Direction) //Just a type alias
    trait ColumnSelector {
      val cols: Map[String, Rep[_]] //The runtime map between string names and table columns
    }
    implicit class MultiSortableQuery[A <: ColumnSelector, B, C[_]](query: Query[A, B, C]) {
        def dynamicSortBy(sortBy: Seq[ColumnOrdering]): Query[A, B, C]  =
          sortBy.foldRight(query){ //Fold right is reversing order
            case ((sortColumn, sortOrder), queryToSort) =>
              val sortOrderRep: Rep[_] => Ordered = ColumnOrdered(_, Ordering(sortOrder))
              val sortColumnRep: A => Rep[_] = _.cols(sortColumn)
              queryToSort.sortBy(sortColumnRep)(sortOrderRep)
          }
    }
  }

}
