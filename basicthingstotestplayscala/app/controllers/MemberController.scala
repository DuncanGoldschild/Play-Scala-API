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
import models.{MemberCreationRequest, MemberAuth}


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

  // Display the Member by its id with GET /Member/"id"
  def findMemberById(username: String): Action[AnyContent] = Action.async {
    memberRepository.findOne(username)
      .map{
        case Some(member) => Ok(Json.toJson(member))
        case None => NotFound
      }.recover(logAndInternalServerError)
  }

  def authMember : Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[MemberAuth]
    memberResult.fold(
      errors => {
        badRequest(errors)
      },
      memberAuth => {
        memberRepository.findUser(memberAuth).map{
          case Some(createdMember) => Ok(Json.toJson(jwtService.generateToken(createdMember.username)))
          case None => NotFound
        }.recover(logAndInternalServerError)
      }
    )
  }

  // Display all Member elements with GET /Members
  def allMembers: Action[AnyContent] = Action.async {
    memberRepository.listAll.map{
      list => Ok(Json.toJson(list))
    }.recover(logAndInternalServerError)
  }

  // Delete with DELETE /Member/"id"
  def deleteMember(username : String): Action[AnyContent] =  Action.async {
    memberRepository.deleteOne(username)
      .map{
        case Some(_) => NoContent
        case None => NotFound
      }.recover(logAndInternalServerError)
  }

  // Add with POST /Members
  def createNewMember: Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[MemberCreationRequest]
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

  // Update with PUT /Member/"id"
  def updateMember(username : String): Action[JsValue] = Action.async(parse.json) { request =>
    val memberResult = request.body.validate[MemberCreationRequest]
    memberResult.fold(
      errors => {
        badRequest(errors)
      },
      member => {
        memberRepository.updateOne(username,member)
          .map {
            case Some(_) => NoContent
            case None => NotFound
          }.recover(logAndInternalServerError)
      }
    )
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