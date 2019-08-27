package com.snapptrip.utils.formatters

object EmailFormatter {

  def format(email: Option[String]): Option[String] = {

    email.map(_.trim).flatMap{ em =>
      if(em.length == 0 || !em.contains("@")){
        None
      } else {
        val e = em.split("@").toList
        Some((if(e.last == "gmail.com") {
          e.head.replace(".", "") + "@" + e.last
        } else {
          em
        }).toLowerCase)
      }
    }

  }

}
