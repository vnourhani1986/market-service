package com.snapptrip.utils.formatters

object MobileNoFormatter {

  val formats = List(("""[0][9][0-9][0-9]{8,8}""", 1), ("""[0][0][9][8][9][0-9][0-9]{8,8}""", 4), ("""[+][9][8][9][0-9][0-9]{8,8}""", 3))

  val mapper = Map(
    ('+','+'),
    ('۰','0'),
    ('۱','1'),
    ('۲','2'),
    ('۳','3'),
    ('۴','4'),
    ('۵','5'),
    ('۶','6'),
    ('۷','7'),
    ('۸','8'),
    ('۹','9'),
    ('0','0'),
    ('1','1'),
    ('2','2'),
    ('3','3'),
    ('4','4'),
    ('5','5'),
    ('6','6'),
    ('7','7'),
    ('8','8'),
    ('9','9'),
  )

  def format(mobileNo: String) = {
    val mobile = noPersianToEnglish(mobileNo.trim)
    formats.map(x => (mobile.matches(x._1), x)).find(_._1).map(_._2._2).map(mobile.substring).getOrElse(mobile)
  }

  def noPersianToEnglish(no: String): String = {
    no.toCharArray.toList.map(x => mapper.getOrElse(x, "0")).mkString
  }

}