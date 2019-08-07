package utils

import javax.inject.Inject
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.mvc._
import services.{BCryptServiceImpl, JwtServiceImpl}

import scala.collection.Seq
import scala.concurrent._

class ControllerUtils @Inject() (
                                  components: ControllerComponents
                                ) extends AbstractController(components) {
  val jwtService = new JwtServiceImpl
  val bcryptService = new BCryptServiceImpl

  val logger = Logger(this.getClass)

  def badRequest(errors: Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] = {
    Future.successful(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(errors))))
  }

  def logAndInternalServerError: PartialFunction[Throwable, Result] = {
    case e: Throwable =>
      logger.error(e.getMessage, e)
      InternalServerError
  }

  def createIdAndLabelElementJsonLink(id: String, label: String, name: String, uri: String, verb: String, mediaType: String) = {
    Json.obj(
      "id" -> id,
      "label" -> label,
      "@controls" -> Json.obj(
        name -> Json.obj(
          "href" -> uri,
          "verb" -> verb,
          "mediaType" -> mediaType
        )
      )
    )
  }
}


class UserRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

class AppAction @Inject()(val parser: BodyParsers.Default)
                         (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent] {

    val jwtService = new JwtServiceImpl

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    request.headers.get("Authorization")
      .map{
        token: String => jwtService.getUsernameFromToken(token)
          .map {
            username: String => block(new UserRequest[A](username, request))
          }.getOrElse(unauthorized)
      }.getOrElse(unauthorized)
  }

  private def unauthorized = Future.successful {Results.Status(Status.UNAUTHORIZED)}
}

