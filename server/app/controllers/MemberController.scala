package controllers


import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import hypermedia.{Hypermedia, HypermediaControl, Resource}
import metadata.Http
import repositories.MongoMemberRepository
import models.{ForbiddenException, Member, MemberUpdateRequest, NotFoundException, Token}
import utils.{AuthenticatedAction, ControllerUtils}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class MemberController @Inject() (
                                  components: ControllerComponents,
                                  memberRepository: MongoMemberRepository,
                                  appAction: AuthenticatedAction
                                 ) extends AbstractController(components) {

  // Display the Member by its username with GET /member/{username}
  def findMemberById(username: String): Action[AnyContent] = appAction.async {
    memberRepository.findOne(username)
      .map {
        case Some(member) => Ok(Json.obj("info" -> member))
        case None => NotFound
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  def authMember: Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
      memberResult.fold(
        ControllerUtils.badRequest,
        memberAuth => {
          memberRepository.auth(memberAuth).map {
            case Some(token) => Ok(Hypermedia.writeResponseDocument(
              Token(token), generateHypermediaMember(memberAuth.username)
            ))
            case None => BadRequest(createMemberHypermediaControl.toJson)
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
  def deleteMember(username: String): Action[AnyContent] = appAction.async(parse.default) { request =>
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
          memberRepository.createOne(member).map {
            case Some(createdMember) =>
              Created(Hypermedia.writeResponseDocument(
                createdMember, generateHypermediaMember(createdMember.username)
              ))
            case None => BadRequest("Username already exists")
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
  private def generateHypermediaMember(username: String): Map[String, HypermediaControl] =
    Map(
      Hypermedia.createControl("self", "Self informations", routes.MemberController.findMemberById(username), Http.MediaType.JSON),
      Hypermedia.createControl("auth", "Authenticate a member", routes.MemberController.authMember(), Http.MediaType.JSON, Schemas.authSchema),
      Hypermedia.createControl("changePassword", "Update your password", routes.MemberController.updateMember(username), Http.MediaType.JSON, Schemas.updatePasswordSchema),
      Hypermedia.createControl("delete", "Delete your account", routes.MemberController.deleteMember(username), Http.MediaType.JSON),
      Hypermedia.createControl("getBoards", "Get all your boards", routes.BoardController.allUserBoards(), Http.MediaType.JSON)
    )

  private lazy val createMemberHypermediaControl = {
    new Resource(Hypermedia.createControl("createMember", "Create a new account", routes.MemberController.createNewMember(), Http.MediaType.JSON))
  }

}