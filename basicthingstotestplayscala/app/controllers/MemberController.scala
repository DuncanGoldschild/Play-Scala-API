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

import services.JwtTokenGenerator

import models.{Member, MemberUpdateRequest}



/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class MemberController @Inject() (
                                  components: ControllerComponents,
                                  memberRepository: MongoMemberRepository,
                                  jwtService : JwtTokenGenerator
                                ) extends AbstractController(components) {

  private val logger = Logger(this.getClass)

  // Display the Member by its id with GET /member/"id"
  def findMemberById(username: String): Action[AnyContent] = Action.async {
    memberRepository.findOne(username)
      .map{
        case Some(member) => Ok(Json.toJson(member))
        case None => NotFound
      }.recover(logAndInternalServerError)
  }

  def authMember : Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
    memberResult.fold(
      errors => {
        badRequest(errors)
      },
      memberAuth => {
        memberRepository.findByUsername(memberAuth.username).map{
          case Some(member) =>
            if (member.password == memberAuth.password) Ok(Json.toJson(jwtService.generateToken(member.username)))
            else Unauthorized
          case None => NotFound
        }.recover(logAndInternalServerError)
      }
    )
  }

  // Display all Member elements with GET /members
  def allMembers: Action[AnyContent] = Action.async {
    memberRepository.listAll.map{
      list => Ok(Json.toJson(list))
    }.recover(logAndInternalServerError)
  }

  // Delete with DELETE /member/"id"
  def deleteMember(username : String): Action[JsValue] =  Action.async(parse.json) { request =>
    checkUserPermissions(request, username)
        match {
          case Some(_) =>
            memberRepository.deleteOne(username)
              .map {
                case Some(_) => NoContent
                case None => NotFound
              }.recover(logAndInternalServerError)
          case None => Future.successful(Unauthorized)
        }
  }

  // Add with POST /members
  def createNewMember: Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[Member]
    memberResult.fold(
      errors => {
        badRequest(errors)
      },
      member => {
        memberRepository.createOne(member).map{
          case Some(createdMember) => Ok(Json.toJson(createdMember))
          case None => BadRequest("Username already exists")
        }.recover(logAndInternalServerError)
      }
    )
  }

  // Update with PUT /member/"username"
  def updateMember(username : String): Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[MemberUpdateRequest]
    memberResult.fold(
      errors => {
        badRequest(errors)
      },
      member => {
        checkUserPermissions(request, username)
        match {
          case Some(_) =>
            memberRepository.updateOne(username, member)
              .map {
                case Some(_) => NoContent
                case None => NotFound
              }.recover(logAndInternalServerError)
          case None => Future.successful(Unauthorized)
        }
      }
    )
  }

  private def checkUserPermissions (request : Request[JsValue], username : String) : Option[Unit] = {
    request.headers.get("Authorization") match {
      case Some(token: String) => {
        jwtService.fetchUsername(token) match {
            case Some(tokenUsername) => if ( tokenUsername == username) Some(Unit) else None
            case None => None
          }
      }
      case None => None
    }
  }

  private def badRequest (errors : Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] = {
    Future.successful(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(errors))))
  }

  private def logAndInternalServerError: PartialFunction[Throwable, Result] = {
    case e : Throwable =>
      logger.error(e.getMessage, e)
      InternalServerError
  }
}