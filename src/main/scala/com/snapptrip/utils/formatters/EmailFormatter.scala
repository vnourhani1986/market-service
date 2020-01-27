package com.snapptrip.utils.formatters

import com.snapptrip.utils.EmailAddress

import scala.util.matching.Regex

object EmailFormatter {

  def format(email: Option[String]): Option[String] = {

    email.map(_.trim).flatMap { em =>
      if (em.length == 0 || !em.contains("@") || !EmailAddress.isValid(em)) {
        None
      } else {
        val e = em.split("@").toList
        Some((if (e.last == "gmail.com") {
          e.head.replace(".", "") + "@" + e.last
        } else {
          em
        }).toLowerCase)
      }
    }

  }

}
