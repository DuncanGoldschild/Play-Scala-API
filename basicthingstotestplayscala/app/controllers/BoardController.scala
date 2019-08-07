package controllers

import scala.concurrent._

import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton

import repositories.{MongoBoardRepository, MongoMemberRepository, MongoTasksListRepository}
import models.{Board, BoardCreationRequest, BoardUpdateRequest, ForbiddenException, NotFoundException}
import utils.{AppAction, ControllerUtils, UserRequest}



@Singleton
class BoardController @Inject() (
                                 components: ControllerComponents,
                                 boardRepository: MongoBoardRepository,
                                 memberRepository: MongoMemberRepository,
                                 tasksListRepository: MongoTasksListRepository,
                                 appAction: AppAction
                               ) extends AbstractController(components) {

  val controllerUtils = new ControllerUtils(components)

  // Returns a JSON of the board by its id with GET /board/{id}
  def findBoardById(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    boardRepository.find(id, request.username)
      .flatMap {
        case Right(board) =>
          addListsAndControlsRoutesAndOk(id, board)
        case Left(_: NotFoundException) => Future.successful(NotFound)
        case Left(_: ForbiddenException) => Future.successful(Forbidden)
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Returns a JSON of all board elements with GET /boards
  def allUserBoards: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    val username = request.username
    boardRepository.listAllFromUsername(username)
      .map {
        listOfBoards => addBoardsHypermediaAndOk(listOfBoards, username)
      }
  }

  // Delete with DELETE /board/{id}
  def deleteBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    boardRepository.delete(id, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue]=>
    request.body.validate[BoardCreationRequest]
      .fold(
        controllerUtils.badRequest,
        board => {
          boardRepository.createOne(board, request.username).map {
            createdBoard: Board => Created(Json.toJson(createdBoard))
          }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /board/{id}
  def updateBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[BoardUpdateRequest]
      .fold(
        controllerUtils.badRequest,
        boardUpdateRequest => {
          boardRepository.update(id, boardUpdateRequest, request.username)
            .map {
              case Right(_) => NoContent
              case Left(_: NotFoundException) => NotFound
              case Left(_: ForbiddenException) => Forbidden
            }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  // Returns a list of all the lists contained in this board
  private def addListsAndControlsRoutesAndOk(id: String, board: Board): Future[Result] =
    tasksListRepository.listAllListsFromBoardId(id)
      .map {
        listOfBoardLists =>
          val listSelfMethods: List[JsObject] =
            createCRUDActionJsonLink("self", routes.BoardController.findBoardById(id).toString, "GET", "application/json") ::
              createCRUDActionJsonLink("delete", routes.BoardController.deleteBoard(id).toString, "DELETE", "application/json") :: List()
          var listTasksList: List[JsObject] = List()
          for (tasksList <- listOfBoardLists)
            listTasksList = controllerUtils.createIdAndLabelElementJsonLink(tasksList.id, tasksList.label, "get", routes.TasksListController.findListTaskById(tasksList.id).toString, "GET", "application/json") :: listTasksList
          Ok(Json.obj("info" -> Json.toJson(board), "lists" -> listTasksList, "@controls" -> listSelfMethods))
      }

  private def addBoardsHypermediaAndOk (listOfBoards: List[Board], username: String): Result = {
    val listSelfMethods: List[JsObject] =
      createCRUDActionJsonLink("self", routes.BoardController.allUserBoards().toString, "GET", "application/json") :: createCRUDActionJsonLink("create", routes.BoardController.createNewBoard.toString, "POST", "application/json") :: List()
    var listBoards: List[JsObject] = List()
    for (board <- listOfBoards)
      listBoards = controllerUtils.createIdAndLabelElementJsonLink(board.id, board.label, "get", routes.BoardController.findBoardById(board.id).toString, "GET", "application/json") :: listBoards
    Ok(Json.obj("username" -> username, "boards" -> listBoards, "@controls" -> listSelfMethods))
  }

  private def createCRUDActionJsonLink(name: String, uri: String, verb: String, mediaType: String) = {
    Json.obj(
      name -> Json.obj(
        "href" -> uri,
        "verb" -> verb,
        "mediaType" -> mediaType
      )
    )
  }
}