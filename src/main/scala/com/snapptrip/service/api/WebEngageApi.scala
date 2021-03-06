package com.snapptrip.service.api

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import com.snapptrip.DI._
import com.snapptrip.utils.WebEngageConfig
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsValue

import scala.concurrent.Future

object WebEngageApi extends LazyLogging {

  def post(request: JsValue, url: String): Future[(StatusCode, JsValue)] = {

    for {
      body <- Marshal(request).to[RequestEntity]
      request = HttpRequest(HttpMethods.POST, url)
        .withHeaders(RawHeader("Authorization", WebEngageConfig.apiKey))
        .withEntity(body.withContentType(ContentTypes.`application/json`))
      connectionFlow = Http().outgoingConnectionHttps(WebEngageConfig.host, settings = WebEngageConfig.clientConnectionSettings)
      (status, responseBody) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
      entity <- Unmarshal(responseBody).to[JsValue]
    } yield {
      (status, entity)
    }
  }

  def get(url: String): Future[(StatusCode, JsValue)] = {

    for {
      request <- Future.successful(HttpRequest(HttpMethods.GET, url)
        .withHeaders(RawHeader("Authorization", WebEngageConfig.apiKey)))
      connectionFlow = Http().outgoingConnectionHttps(WebEngageConfig.host, settings = WebEngageConfig.clientConnectionSettings)
      (status, responseBody) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
      entity <- Unmarshal(responseBody).to[JsValue]
    } yield {
      (status, entity)
    }
  }

  def delete(url: String): Future[(StatusCode, JsValue)] = {

    for {
      request <- Future.successful(HttpRequest(HttpMethods.DELETE, url)
        .withHeaders(RawHeader("Authorization", WebEngageConfig.apiKey)))
      connectionFlow = Http().outgoingConnectionHttps(WebEngageConfig.host, settings = WebEngageConfig.clientConnectionSettings)
      (status, responseBody) <- Source.single(request).via(connectionFlow).runWith(Sink.head).map(x => (x.status, x.entity))
      entity <- Unmarshal(responseBody).to[JsValue]
    } yield {
      (status, entity)
    }
  }

}