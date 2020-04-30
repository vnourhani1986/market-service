package com.snapptrip.utils.formatters

object EmailFormatter {

  def format(email: Option[String]): Option[String] = {

    val e = email.map(_.trim.toLowerCase)
    e.map(_.matches("""^([a-zA-Z0-9_\-\.]+)@([a-zA-Z0-9_\-\.]+)\.([a-zA-Z]{2,5})$""")) match {
      case Some(b) if b => e
      case _ => None
    }

  }

}
