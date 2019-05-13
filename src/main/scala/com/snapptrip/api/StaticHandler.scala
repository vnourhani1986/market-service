package com.snapptrip.api

import java.nio.file.Paths

import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse}
import akka.stream.scaladsl.FileIO
import com.snapptrip.utils.ConfigHolder

object StaticHandler {

  def loadStaticFile(fileName: String, contentType: ContentType): HttpResponse = {
    val path = s"${ConfigHolder.temp_folder}$fileName"
    val file = Paths.get(path)
    val entity = HttpEntity(contentType, FileIO.fromPath(file))
    HttpResponse(entity = entity)
  }
}
