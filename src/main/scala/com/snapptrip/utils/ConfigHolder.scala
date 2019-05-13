package com.snapptrip.utils

import com.snapptrip.DI._

object ConfigHolder {
  val temp_folder: String = config.getString("csv.temp_directory")
}
