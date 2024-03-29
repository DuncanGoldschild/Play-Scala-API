package utils

import javax.inject.Inject
import play.api.mvc.{ActionBuilder, AnyContent, BodyParsers, Request, Result, Results, WrappedRequest}
import services.DefaultJwtService

import scala.concurrent.{ExecutionContext, Future}

class RequestWithAuth[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)
class AuthenticatedAction @Inject()(val parser: BodyParsers.Default)
                                   (implicit val executionContext: ExecutionContext)
  extends ActionBuilder[RequestWithAuth, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: RequestWithAuth[A] => Future[Result]): Future[Result] = {
    request.headers.get("Authorization")
      .flatMap(DefaultJwtService.getUsernameFromToken)
      .map { username => block(new RequestWithAuth[A](username, request)) }
      .getOrElse(unauthorized)
  }

  private def unauthorized = Future.successful { Results.Unauthorized }

}

