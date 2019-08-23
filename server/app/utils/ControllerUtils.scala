package utils

import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.mvc._

import scala.collection.Seq
import scala.concurrent._

object ControllerUtils {

  val logger = Logger(this.getClass)

  def badRequest(errors: Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] = {
    Future.successful(Results.BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(errors))))
  }

  def logAndInternalServerError: PartialFunction[Throwable, Result] = {
    case e: Throwable =>
      logger.error(e.getMessage, e)
      Results.Status(Status.INTERNAL_SERVER_ERROR)
  }

}
