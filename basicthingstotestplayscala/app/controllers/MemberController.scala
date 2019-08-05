package controllers


import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.MongoMemberRepository
import services.{BCryptServiceImpl}
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

  val controllerUtils = new ControllerUtils(components)

  // Display the Member by its username with GET /member/{username}
  def findMemberById(username: String): Action[AnyContent] = appAction.async {
    memberRepository.findOne(username)
      .map {
        case Some(member) => Ok(Json.toJson(member))
        case None => NotFound
      }.recover(controllerUtils.logAndInternalServerError)
  }

  def authMember: Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
      memberResult.fold(
        controllerUtils.badRequest,
        memberAuth => {
          memberRepository.auth(memberAuth).map {
            case Some(token) =>

              Ok(Json.toJson(token))
            case None => BadRequest("Invalid username or password")
          }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  // Display all Member elements with GET /members
  def allMembers: Action[AnyContent] = appAction.async {
    memberRepository.listAll.map {
      list => Ok(Json.toJson(list))
    }.recover(controllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /member/{username}
  def deleteMember(username: String): Action[JsValue] = appAction.async(parse.json) { request =>
    memberRepository.delete(username, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /members
  def createNewMember: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Member]
      .fold(
        controllerUtils.badRequest,
        member => {
          memberRepository.createOne(member).map {
            case Some(createdMember) => Created(Json.toJson(createdMember))
            case None => BadRequest("Username already exists")
          }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /member/{username}
  def updateMember(username: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[MemberUpdateRequest]
      .fold(
        controllerUtils.badRequest,
        memberUpdateRequest => {
          memberRepository.update(username, request.username, memberUpdateRequest)
            .map {
              case Right(_) => NoContent
              case Left(_: NotFoundException) => NotFound
              case Left(_: ForbiddenException) => Forbidden
            }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }
}