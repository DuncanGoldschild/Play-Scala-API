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
import repositories.{MongoBoardRepository, MongoMemberRepository}
import services.JwtTokenGenerator
import models.{Board, BoardCreationRequest, BoardUpdateRequest, Member}

import scala.util.{Failure, Success}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class BoardController @Inject() (
                                 components: ControllerComponents,
                                 boardRepository: MongoBoardRepository,
                                 memberRepository: MongoMemberRepository,
                                 jwtService : JwtTokenGenerator
                               ) extends AbstractController(components) {

  private val logger = Logger(this.getClass)

  // Display the board by its id with GET /board/"id"
  def findBoardById(id: String): Action[JsValue] = Action.async(parse.json) { request : Request[JsValue] =>
    request.headers.get("Authorization") match {
      case Some(token : String) => {
        jwtService.verifyToken(token) match{
          case Success(_) =>  boardRepository.findOne(id)
            .flatMap{
              case Some(board : Board) =>
                if (board.membersUsername.contains(jwtService.fetchPayload(token))) Future.successful(Ok(Json.toJson(board)))
                else Future.successful(NotFound)
              case None => Future.successful(Unauthorized)
            }.recover(logAndInternalServerError)
          case Failure(_) => Future.successful(Unauthorized)
        }
      } // handle token auth
      case None => Future.successful(Unauthorized)
    }
  }

  // Display all board elements with GET /boards
  def allUserBoards: Action[JsValue] = Action.async(parse.json) { request : Request[JsValue] =>
    request.headers.get("Authorization") match {
      case Some(token : String) => {
        jwtService.verifyToken(token) match{
          case Success(_) =>  boardRepository.listAllFromUsername(jwtService.fetchPayload(token))
            .map{
               list => Ok(Json.toJson(list))
                }.recover(logAndInternalServerError)
          case Failure(_) => Future.successful(Unauthorized)
        }
      }
      case None => Future.successful(Unauthorized)
    }
  }

  // Delete with DELETE /board/"id"
  def deleteBoard(id : String): Action[JsValue] =  Action.async(parse.json) { request : Request[JsValue] =>
    request.headers.get("Authorization") match {
      case Some(token : String) => {
        jwtService.verifyToken(token) match{
          case Success(_) =>  boardRepository.findOne(id)
            .flatMap{
              case Some(board : Board) =>
                if (board.membersUsername.contains(jwtService.fetchPayload(token))) boardRepository.deleteOne(id)
                  .map{
                    case Some(_) => NoContent
                    case None => NotFound
                  }.recover(logAndInternalServerError)
                else Future.successful(NotFound)
              case None => Future.successful(Unauthorized)
            }.recover(logAndInternalServerError)
          case Failure(_) => Future.successful(Unauthorized)
        }
      } // handle token auth
      case None => Future.successful(Unauthorized)
    }
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = Action.async(parse.json) { request =>
    val boardResult = request.body.validate[BoardCreationRequest]
    boardResult.fold(
      errors => {
        badRequest(errors)
      },
      board => {
        boardRepository.createOne(board, "JeanMich").map{ // TODO : username request
          createdBoard => Ok(Json.toJson(createdBoard))
        }.recover(logAndInternalServerError)
      }
    )
  }

  // Update with PUT /board/"id"
  def updateBoard(id : String): Action[JsValue] = Action.async(parse.json) { request =>
    val boardResult = request.body.validate[BoardUpdateRequest]
    boardResult.fold(
      errors => {
        badRequest(errors)
      },
      board => {
        boardRepository.updateOne(id,board)
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