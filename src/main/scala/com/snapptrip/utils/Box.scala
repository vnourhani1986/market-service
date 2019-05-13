package com.snapptrip.utils

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, onSuccess}
import akka.http.scaladsl.server.Route
import com.snapptrip.DI.ec
import com.snapptrip.formats.Formats._
import scalaz.\/
import spray.json._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

trait BoxComponent {

  import scalaz.EitherT.eitherT
  import scalaz.{-\/, EitherT, Monad, \/-}

  val Valid: \/-.type = \/-
  val Fault: -\/.type = -\/

  type Box[T] = EitherT[Future, MetaData, T]

  type Value[T] = MetaData \/ T

  implicit val FutureMonad: Monad[Future] = new Monad[Future] {
    override def bind[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa flatMap f

    override def point[A](a: => A): Future[A] = Future.successful(a)
  }

  trait BoxMagnet[T] {
    def invoke(): Box[T]
  }

  def toBox[T](magnet: BoxMagnet[T]): Box[T] = magnet.invoke()

  def failedBox[T](metaData: MetaData): Box[T] = eitherT(Future[Value[T]](Fault(metaData)))

  implicit def entityToBoxMagnet[T](t: T): BoxMagnet[T] = () => eitherT(Future[Value[T]](Valid(t)))

  implicit def futureToBoxMagnet[T](t: Future[T]): BoxMagnet[T] = () => {
    eitherT[Future,MetaData,T](t.transform {
      case Success(value) => Success(Valid(value))
      case Failure(e) => Failure(e)
    })
  }


  def ifExists[T](optional: Future[Option[T]])(implicit metaData: MetaData): Box[T] = {
    eitherT(optional.transform {
      case Success(Some(value)) => Success(Valid(value))
      case Success(None) => Success(Fault(metaData withStatus StatusCodes.NotFound))
      case Failure(e) => Failure(e)
    })
  }

  def ifValid(bool: Future[Boolean])(implicit metaData: MetaData): Box[Boolean] = {
    eitherT(bool.transform {
      case Success(true) => Success(Valid(true))
      case Success(false) => Success(Fault(metaData))
      case Failure(e) => Failure(e)
    })
  }

  implicit class BoxExtensions[T](box: Box[T]) {

    def ensureWith(f: T => Boolean)(fault: T => MetaData): Box[T] = {
      box.flatMap[T] {
        case st if f(st) => box
        case sf => failedBox[T](fault(sf))
      }
    }

    def transform(f: Try[Value[T]] => Try[Value[T]]): Box[T] = {
      eitherT(box.run.transform(f))
    }

    def replaceLeft(metaData: MetaData): Box[T] = {
      eitherT(box.run.transform {
        case Success(value) =>
          if (value.isLeft) Success(Fault(metaData))
          else Success(value)
        case Failure(t) => Failure(t)
      })
    }

    def failureToMeta(metaData: MetaData): Box[T] = {
      eitherT(box.run.transform {
        case Success(value) => Success(value)
        case Failure(_) => Success(Fault(metaData))
      })
    }

  }

  def invalidRequestParams[T](metaData: MetaData): Box[T] = failedBox[T](metaData withStatus StatusCodes.UnprocessableEntity)


  def notFound()(implicit metaData: MetaData): MetaData = metaData withStatus StatusCodes.NotFound withMessage "entity not found"

  def invalidData()(implicit metaData: MetaData): MetaData = metaData withStatus StatusCodes.UnprocessableEntity withMessage "invalid data"

  def unAuthorized(): MetaData = MetaData() withStatus StatusCodes.Unauthorized withMessage "not authorized"

  def badRequest()(implicit metaData: MetaData): MetaData = metaData withStatus StatusCodes.BadRequest

  def serverError()(implicit metaData: MetaData): MetaData = metaData withStatus StatusCodes.InternalServerError withMessage "server error"

  def requestTimeout()(implicit metaData: MetaData): MetaData = metaData withStatus StatusCodes.RequestTimeout withMessage "request timeout"


}

case class MetaData(entity: Option[String] = None,
                    id: Option[String] = None,
                    action: Option[String] = None,
                    message: String = "error while handling request",
                    oldData: Option[String] = None,
                    newData: Option[String] = None,
                    payloads: List[String] = Nil,
                    status: Int = StatusCodes.InternalServerError.intValue,
                    errorCode: Option[String] = None
                   ) {
  self =>

  def withEntity(e: String): MetaData = self.copy(entity = Some(e))

  def withMessage(m: String): MetaData = self.copy(message = m)

  def withAction(action: String): MetaData = self.copy(action = Some(action))

  def withId(id: String): MetaData = self.copy(id = Some(id))

  def withOldData(oldData: String): MetaData = self.copy(oldData = Some(oldData))

  def withNewData(newData: String): MetaData = self.copy(newData = Some(newData))

  def withPayloads(payloads: List[String]): MetaData = self.copy(payloads = payloads)

  def withStatus(status: StatusCode): MetaData = self.copy(status = status.intValue())

  def withCode(errorCode: String): MetaData = self.copy(errorCode = Some(errorCode), message = "") //Errors.findDetails(errorCode))
 //
  def toHttpResponse: HttpResponse = {

    HttpResponse(
      status = self.status,
      entity = HttpEntity(ContentTypes.`application/json`, self.asInstanceOf[MetaData].toJson.compactPrint)
    )

  }

}

trait BoxToResponseComponent extends BoxComponent {
  implicit def boxToResponse[T](result: Box[T])(implicit encoder: JsonWriter[T]): Route = {
    onSuccess(result.run) {
      case Valid(value) =>
        val response =
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(ContentTypes.`application/json`, value.toJson.compactPrint)
          )
        complete(response)
      case Fault(metaData: MetaData) =>
        complete(metaData.toHttpResponse)
    }
  }

}
