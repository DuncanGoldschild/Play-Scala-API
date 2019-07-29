package controllers


import scala.concurrent._


import javax.inject._

import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.libs.json._

import com.google.inject.Singleton

import repositories.{MongoBoardRepository, MongoMemberRepository}

import services.JwtGenerator

import models.{Board, BoardCreationRequest, BoardUpdateRequest}


@Singleton
class BoardController @Inject() (
                                 components: ControllerComponents,
                                 boardRepository: MongoBoardRepository,
                                 memberRepository: MongoMemberRepository,
                                 jwtService: JwtGenerator,
                                 controllerUtils: ControllerUtils,
                                 appAction: AppAction
                               ) extends AbstractController(components) {

  // Returns a JSON of the board by its id with GET /board/"id"
  def findBoardById(id: String): Action[JsValue] = appAction.async(parse.json) { request : UserRequest[JsValue] =>
    boardRepository.findOne(id)
      .map {
        case Some(board) if isUsernameContainedInBoard(request.username, board) => Ok(Json.toJson(board))
        case None => NotFound
        case _ => Forbidden
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // TODO : Enlever tous les VERIFY TOKEN et faire attention aux fonctions qui ne nécessitent pas d'authentification (créer un membre)

  // Returns a JSON of all board elements with GET /boards
  def allUserBoards: Action[JsValue] = appAction.async(parse.json) { request: Request[JsValue] =>
    verifyTokenAndGetUsername(request) match {
      case Some(tokenUsername) => {
        boardRepository.listAllFromUsername(tokenUsername)
          .map {
            list => Ok(Json.toJson(list))
          }.recover(controllerUtils.logAndInternalServerError)
        }
      case None => Future.successful(Unauthorized)
    }
  }

  // Delete with DELETE /board/"id"
  def deleteBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request: Request[JsValue] =>
    verifyTokenAndGetUsername(request) match {
      case Some(tokenUsername) => boardRepository.findOne(id)
        .flatMap {
          case Some(board) if isUsernameContainedInBoard(tokenUsername, board) =>
             boardRepository.deleteOne(id)
              .map {
                case Some(_) => NoContent
              }.recover(controllerUtils.logAndInternalServerError)
          case None => Future.successful(NotFound)
          case _ =>  Future.successful(Forbidden)
        }.recover(controllerUtils.logAndInternalServerError)
      case None => Future.successful(Unauthorized)
    }
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = appAction.async(parse.json) { request: Request[JsValue]=>
    verifyTokenAndGetUsername(request)
    match {
      case Some(tokenUsername) =>
        val boardResult = request.body.validate[BoardCreationRequest]
        boardResult.fold(
          controllerUtils.badRequest,
          board => {
            boardRepository.createOne(board, tokenUsername).map {
              createdBoard: Board => Ok(Json.toJson(createdBoard))
            }.recover(controllerUtils.logAndInternalServerError)
          }
        )
      case None => Future.successful(Unauthorized)
    }
  }

  // Update with PUT /board/"id"
  def updateBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    verifyTokenAndGetUsername(request)
      match {
        case Some(tokenUsername) =>
          request.body.validate[BoardUpdateRequest]
            .fold(
              controllerUtils.badRequest,
              boardUpdateRequest => {
                boardRepository.findOne(id)
                  .flatMap {
                    case Some(board: Board) if isUsernameContainedInBoard(tokenUsername, board) =>
                        boardRepository.updateOne(id, boardUpdateRequest)
                          .map {
                            case Some(_) => NoContent
                          }.recover(controllerUtils.logAndInternalServerError)
                    case None => Future.successful(NotFound)
                    case _ => Future.successful(Forbidden)
                  }
              }
            )
        case None => Future.successful(Unauthorized)
      }
  }

  private def verifyTokenAndGetUsername(request: Request[JsValue]): Option[String] = {
    request.headers.get("Authorization")
      .flatMap {
        token: String => jwtService.getUsernameFromToken(token)
      }
  }

  private def isUsernameContainedInBoard (username: String, board: Board): Boolean = board.membersUsername.contains(username)
}