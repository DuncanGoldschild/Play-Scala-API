package controllers

import hypermedia.Hypermedia
import javax.inject.Inject
import metadata.Http
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class HomeController @Inject() (components: ControllerComponents) extends AbstractController(components) {

  def welcome: Action[AnyContent] = Action {
    Ok(Json.obj( "@controls" -> HomeController.HYPERMEDIA_CONTROLS))
  }

}

object HomeController {

  lazy val HYPERMEDIA_CONTROLS: List[JsObject] = List(AUTHENTICATION_CONTROL, CREATE_MEMBER_CONTROL)

  lazy val AUTHENTICATION_CONTROL: JsObject = {
    val controls = Hypermedia.createControl(
    "auth",
    "Authenticate",
    routes.MemberController.authMember(),
    Http.MediaType.JSON,
    Schemas.authSchema
  )
    Json.obj(controls._1 -> controls._2)
  }

  lazy val CREATE_MEMBER_CONTROL: JsObject = {
    val controls = Hypermedia.createControl(
      "createMember",
      "Create a new account",
      routes.MemberController.createNewMember(),
      Http.MediaType.JSON,
      Schemas.authSchema
    )
    Json.obj(controls._1 -> controls._2)
  }
}
