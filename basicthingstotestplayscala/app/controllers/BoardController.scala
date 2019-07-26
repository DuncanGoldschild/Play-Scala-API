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

import models.{Board, BoardCreationRequest, BoardUpdateRequest}

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
    verifyTokenAndGetUsername(request)
      match {
        case Some(tokenUsername) =>
          boardRepository.findOne(id)
            .map{
              case Some(board) =>
                if (board.membersUsername.contains(tokenUsername)) Ok(Json.toJson(board))
                else Forbidden
              case None => NotFound
            }.recover(logAndInternalServerError)
        case None => Future.successful(Unauthorized)
      }
  }

  // Display all board elements with GET /boards
  def allUserBoards: Action[JsValue] = Action.async(parse.json) { request: Request[JsValue] =>
    verifyTokenAndGetUsername(request) match {
      case Some(tokenUsername) => {
        boardRepository.listAllFromUsername(tokenUsername)
            .map{
               list => Ok(Json.toJson(list))
                }.recover(logAndInternalServerError)
        }
      case None => Future.successful(Unauthorized)
    }
  }

  // Delete with DELETE /board/"id"
  def deleteBoard(id : String): Action[JsValue] =  Action.async(parse.json) { request: Request[JsValue] =>
    verifyTokenAndGetUsername(request) match{
      case Some(tokenUsername) =>  boardRepository.findOne(id)
        .flatMap{
          case Some(board) =>
            if (board.membersUsername.contains(tokenUsername)) boardRepository.deleteOne(id)
              .map{
                case Some(_) => NoContent
                case None => NotFound
              }.recover(logAndInternalServerError)
            else Future.successful(Forbidden)
          case None => Future.successful(NotFound)
        }.recover(logAndInternalServerError)
      case None => Future.successful(Unauthorized)
    }
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = Action.async(parse.json) { request : Request[JsValue]=>
    verifyTokenAndGetUsername(request)
    match {
      case Some(tokenUsername) =>
        val boardResult = request.body.validate[BoardCreationRequest]
        boardResult.fold(
          errors => {
            badRequest(errors)
          },
          board => {
            boardRepository.createOne(board, tokenUsername).map {
              createdBoard : Board => Ok(Json.toJson(createdBoard))
            }.recover(logAndInternalServerError)
          }
        )
      case None => Future.successful(Unauthorized)
    }
  }

  // Update with PUT /board/"id"
  def updateBoard(id : String): Action[JsValue] = Action.async(parse.json) { request =>
    verifyTokenAndGetUsername(request)
      match {
      case Some(tokenUsername) =>
        request.body.validate[BoardUpdateRequest]
          .fold(
            errors => {
              badRequest(errors)
            },
            boardUpdate => {
              boardRepository.findOne(id)
                .flatMap {
                  case Some(board: Board) =>
                    if (board.membersUsername.contains(tokenUsername)) {
                      boardRepository.updateOne(id, boardUpdate)
                        .map {
                          case Some(_) => NoContent
                          case None => NotFound
                        }.recover(logAndInternalServerError)
                    }
                    else Future.successful(Forbidden)
                  case None => Future.successful(NotFound)
                }

            }
          )
      case None => Future.successful(Unauthorized)
    }
  }

  private def verifyTokenAndGetUsername(request : Request[JsValue]): Option[String] = {
    request.headers.get("Authorization")
    match {
      case Some(token : String) => jwtService.fetchUsername(token)
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