package com.snapptrip.utils


case class EmailAddress(value: String) extends StringValue {

  val (mailbox, domain): (EmailAddress.Mailbox, EmailAddress.Domain) = value match {
    case EmailAddress.validEmail(m, d) => (EmailAddress.Mailbox(m), EmailAddress.Domain(d))
    case invalidEmail => throw new IllegalArgumentException(s"'$invalidEmail' is not a valid email address")
  }

  lazy val obfuscated = ObfuscatedEmailAddress.apply(value)
}

object EmailAddress {
   val validDomain = """^([a-zA-Z0-9-\[]+(?:\.[a-zA-Z0-9-\]]+)*)$""".r
   val validEmail = """^((?!\.)(?!.*?\.\.)[a-zA-Z0-9.!#$%&’'"*+/=?^_`{|}~-]*[a-zA-Z0-9!#$%&’'"*+/=?^_`{|}~-]+)@((?!-)[a-zA-Z0-9-.\[]+\.[a-zA-Z0-9-.\]]+)$""".r

  def isValid(email: String) = email match {
    case validEmail(_,_) => true
    case invalidEmail => false
  }

  case class Mailbox private[EmailAddress] (value: String) extends StringValue
  case class Domain(value: String) extends StringValue {
    value match {
      case EmailAddress.validDomain(_) => //
      case invalidDomain => throw new IllegalArgumentException(s"'$invalidDomain' is not a valid email domain")
    }
  }
}
