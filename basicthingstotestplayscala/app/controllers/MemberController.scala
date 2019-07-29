package controllers


import scala.concurrent._
import scala.collection.Seq

import javax.inject._

import ExecutionContext.Implicits.global
import reactivemongo.play.json._
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger
import com.google.inject.Singleton

import repositories.MongoMemberRepository

import services.JwtGenerator

import models.{Member, MemberUpdateRequest}



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
                                  appAction: AppAction
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

  def authMember : Action[JsValue] = appAction.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
    memberResult.fold(
      errors => {
        controllerUtils.badRequest(errors)
      },
      memberAuth => {
        memberRepository.findByUsername(memberAuth.username).map {
          case Some(member) =>
            if (member.password == memberAuth.password) Ok(Json.toJson(jwtService.generateToken(member.username)))
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
    checkUserPermissions(request, username)
        match {
          case Some(_) =>
            memberRepository.deleteOne(username)
              .map {
                case Some(_) => NoContent
                case None => NotFound
              }.recover(controllerUtils.logAndInternalServerError)
          case None => Future.successful(Unauthorized)
        }
  }

  // Add with POST /members
  def createNewMember: Action[JsValue] = appAction.async(parse.json) { request =>
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
      member => {
        checkUserPermissions(request, username)
          match {
            case Some(_) =>
              memberRepository.updateOne(username, member)
                .map {
                  case Some(_) => NoContent
                  case None => NotFound
                }.recover(controllerUtils.logAndInternalServerError)
            case None => Future.successful(Unauthorized)
          }
      }
    )
  }

  private def checkUserPermissions (request : Request[JsValue], username : String) : Option[Unit] = {
    request.headers.get("Authorization") match {
      case Some(token: String) => {
        jwtService.getUsernameFromToken(token) match {
            case Some(tokenUsername) => if ( tokenUsername == username) Some() else None
            case None => None
          }
      }
      case None => None
    }
  }
}