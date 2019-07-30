package controllers


import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger
import com.google.inject.Singleton
import repositories.MongoMemberRepository
import services.{BCryptServiceImpl, JwtGenerator}
import models.{Member, MemberUpdateRequest}
import utils.{AppAction, ControllerUtils, UserRequest}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class MemberController @Inject() (
                                  components: ControllerComponents,
                                  memberRepository: MongoMemberRepository,
                                  jwtService : JwtGenerator,
                                  controllerUtils: ControllerUtils,
                                  appAction: AppAction,
                                  bcryptService: BCryptServiceImpl
                                ) extends AbstractController(components) {

  private val logger = Logger(this.getClass)

  // Display the Member by its id with GET /member/"id"
  def findMemberById(username: String): Action[AnyContent] = appAction.async {
    memberRepository.findOne(username)
      .map {
        case Some(member) => Ok(Json.toJson(member))
        case None => NotFound
      }.recover(controllerUtils.logAndInternalServerError)
  }

  def authMember : Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
    memberResult.fold(
      errors => {
        controllerUtils.badRequest(errors)
      },
      memberAuth => {
        memberRepository.findByUsername(memberAuth.username).map {
          case Some(member) =>
            if (bcryptService.checkPassword(memberAuth.password, member.password)) Ok(Json.toJson(jwtService.generateToken(member.username)))
            else Unauthorized
          case None => NotFound
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

  // Delete with DELETE /member/"id"
  def deleteMember(username : String): Action[JsValue] =  appAction.async(parse.json) { request =>
    if (checkUserPermissions(request, username))
      memberRepository.deleteOne(username)
        .map {
          case Some(_) => NoContent
          case None => NotFound
        }.recover(controllerUtils.logAndInternalServerError)
    else Future.successful(Unauthorized)
  }

  // Add with POST /members
  def createNewMember: Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
    memberResult.fold(
      errors => {
        controllerUtils.badRequest(errors)
      },
      member => {
        memberRepository.createOne(member).map {
          case Some(createdMember) => Ok(Json.toJson(createdMember))
          case None => BadRequest("Username already exists")
        }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }

  // Update with PUT /member/"username"
  def updateMember(username : String): Action[JsValue] = appAction.async(parse.json) { request =>
    val memberResult = request.body.validate[MemberUpdateRequest]
    memberResult.fold(
      errors => {
        controllerUtils.badRequest(errors)
      },
      memberUpdateRequest => {
        memberRepository.findByUsername(username)
          .flatMap{
            case Some(member) => {
              if (checkUserPermissions(request, username) && bcryptService.checkPassword(memberUpdateRequest.password, member.password))
                memberRepository.updateOne(username, memberUpdateRequest)
                  .map {
                    case Some(_) => NoContent
                  }.recover(controllerUtils.logAndInternalServerError)
              else Future.successful(Unauthorized)
            }
            case None => Future.successful(NotFound)
          }
      }
    )
  }

  private def checkUserPermissions (request : UserRequest[JsValue], username : String) : Boolean = request.username == username
}