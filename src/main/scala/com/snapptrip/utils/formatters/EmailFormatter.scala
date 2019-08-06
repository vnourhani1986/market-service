package com.snapptrip.utils.formatters

object EmailFormatter {

  def format(email: String): String = {
    val e = email.split("@").toList
    (if(e.last == "gmail.com") {
      e.head.replace(".", "") + "@" + e.last
    } else {
      e.head + "@" + e.last
    }).toLowerCase
  }

}