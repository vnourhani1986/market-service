package com.snapptrip.utils.formatters

object EmailFormatter {

  def format(email: Option[String]): Option[String] = {

    val e = email.map(_.trim.toLowerCase)
    if (e.map(_.matches("""^([a-zA-Z0-9_\-\.]+)@([a-zA-Z0-9_\-\.]+)\.([a-zA-Z]{2,5})$""")).get) e else None

  }

}
