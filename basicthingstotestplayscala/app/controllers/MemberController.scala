package controllers


import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.{MongoMemberRepository}
import models.{ForbiddenException, Member, MemberUpdateRequest, NotFoundException}
import utils.{AppAction, ControllerUtils}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class MemberController @Inject() (
                                  components: ControllerComponents,
                                  memberRepository: MongoMemberRepository,
                                  appAction: AppAction
                                 ) extends AbstractController(components) {

  // Display the Member by its username with GET /member/{username}
  def findMemberById(username: String): Action[AnyContent] = appAction.async {
    memberRepository.findOne(username)
      .map {
        case Some(member) => Ok(Json.toJson(member))
        case None => NotFound
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  def authMember: Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
      memberResult.fold(
        ControllerUtils.badRequest,
        memberAuth => {
          memberRepository.auth(memberAuth).flatMap {
            case Some(token) =>
              for {
                memberWithControls <- generateHypermediaMember(memberAuth.username)
              } yield Ok(Json.obj("info" -> tokenJson(token), "@controls" -> memberWithControls))
            case None => Future.successful(BadRequest(createMemberHypermedia))
          }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Display all Member elements with GET /members
  def allMembers: Action[AnyContent] = appAction.async {
    memberRepository.listAll.map {
      list => Ok(Json.toJson(list))
    }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /member/{username}
  def deleteMember(username: String): Action[JsValue] = appAction.async(parse.json) { request =>
    memberRepository.delete(username, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Add with POST /members
  def createNewMember: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Member]
      .fold(
        ControllerUtils.badRequest,
        member => {
          memberRepository.createOne(member).flatMap {
            case Some(createdMember) =>
              for {
                memberWithControls <- generateHypermediaMember(createdMember.username)
              } yield Created(Json.obj("info" -> Json.toJson(createdMember), "@controls" -> memberWithControls))
            case None => Future.successful(BadRequest("Username already exists"))
          }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /member/{username}
  def updateMember(username: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[MemberUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        memberUpdateRequest => {
          memberRepository.update(username, request.username, memberUpdateRequest)
            .map {
              case Right(_) => NoContent
              case Left(_: NotFoundException) => NotFound
              case Left(_: ForbiddenException) => Forbidden
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Returns a JSON object with hypermedia links
  private def generateHypermediaMember(username: String): Future[List[JsObject]] = {
    Future.successful(
      ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.MemberController.findMemberById(username).toString, "GET", "application/json") ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("auth", "Authenticate a member", routes.MemberController.authMember.toString, "POST", "application/json", routes.Schemas.authSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("changePassword", "Update your password", routes.MemberController.updateMember(username).toString, "PUT", "application/json", routes.Schemas.updatePasswordMemberSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLink("delete", "Delete your account", routes.MemberController.deleteMember(username).toString, "DELETE", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("getBoards", "Get all your boards", routes.BoardController.allUserBoards.toString, "GET", "application/json") :: List()
    )
  }
  private def tokenJson(token: String) = Json.obj("token" -> token)

  private def createMemberHypermedia = {
    Json.obj("@controls" -> ControllerUtils.createCRUDActionJsonLink("createMember", "Create a new account", routes.MemberController.createNewMember.toString, "POST", "application/json"))
  }
}