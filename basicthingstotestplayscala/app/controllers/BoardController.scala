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
import utils.{AppAction, ControllerUtils, UserRequest}


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

  // Returns a JSON of all board elements with GET /boards
  def allUserBoards: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
        boardRepository.listAllFromUsername(request.username)
          .map {
            list => Ok(Json.toJson(list))
          }.recover(controllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /board/"id"
  def deleteBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    boardRepository.findOne(id)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(request.username, board) =>
          boardRepository.deleteOne(id)
            .map {
              case Some(_) => NoContent
            }.recover(controllerUtils.logAndInternalServerError)
        case None => Future.successful(NotFound)
        case _ =>  Future.successful(Forbidden)
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue]=>
    request.body.validate[BoardCreationRequest]
      .fold(
        controllerUtils.badRequest,
        board => {
          boardRepository.createOne(board, request.username).map {
            createdBoard: Board => Ok(Json.toJson(createdBoard))
          }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /board/"id"
  def updateBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[BoardUpdateRequest]
      .fold(
        controllerUtils.badRequest,
        boardUpdateRequest => {
          boardRepository.findOne(id)
            .flatMap {
              case Some(board: Board) if isUsernameContainedInBoard(request.username, board) =>
                boardRepository.updateOne(id, boardUpdateRequest)
                  .map {
                    case Some(_) => NoContent
                  }.recover(controllerUtils.logAndInternalServerError)
              case None => Future.successful(NotFound)
              case _ => Future.successful(Forbidden)
            }
        }
      )
  }

  private def isUsernameContainedInBoard (username: String, board: Board): Boolean = board.membersUsername.contains(username)
}