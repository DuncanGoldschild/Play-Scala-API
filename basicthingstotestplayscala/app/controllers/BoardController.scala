package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.{MongoBoardRepository, MongoMemberRepository, MongoTasksListRepository}
import models.{BadRequestException, Board, BoardCreationRequest, BoardUpdateRequest, ForbiddenException, MemberAddOrDelete, NotFoundException, TasksList}
import utils.{AppAction, ControllerUtils, UserRequest}



@Singleton
class BoardController @Inject() (
                                 components: ControllerComponents,
                                 boardRepository: MongoBoardRepository,
                                 memberRepository: MongoMemberRepository,
                                 tasksListRepository: MongoTasksListRepository,
                                 appAction: AppAction
                               ) extends AbstractController(components) {

   // Returns a JSON of the board by its id with GET /board/{id}
  def findBoardById(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    boardRepository.find(id, request.username)
      .flatMap {
        case Right(board) =>
          for {//todo for comprehension pour tous les Addroutesmachin
            lists <- tasksListRepository.listAllListsFromBoardId(id)
            listsWithControls <- generateHypermediaListsControls(lists)
            boardControls <- generateHypermediaBoardControls(board)
          } yield Ok(Json.obj("info" -> Json.toJson(board), "lists" -> listsWithControls, "@controls" -> boardControls))
        case Left(_: NotFoundException) => Future.successful(NotFound)
        case Left(_: ForbiddenException) => Future.successful(Forbidden)
      }.recover(ControllerUtils.logAndInternalServerError)
  }

/* ça sert plus à rien mais c'est stylé comme façon de faire

  def findBoardById2(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    val res: Future[Result] =
      for {
        board <- boardRepository.find(id, request.username)
        lists <- tasksListRepository.listAllListsFromBoardId(id)
        listsWithControls <- generateHypermediaListsControls(lists)
        boardControls <- generateHypermediaBoardControls(board)
      } yield Ok(Json.obj("info" -> Json.toJson(board), "lists" -> listsWithControls, "@controls" -> boardControls))

    res.recover {
      case _: ForbiddenException => Forbidden
      case _: NotFoundException => NotFound
    }
      .recover(ControllerUtils.logAndInternalServerError)
  }*/


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
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue]=>
    request.body.validate[BoardCreationRequest]
      .fold(
        ControllerUtils.badRequest,
        board => {
          boardRepository.createOne(board, request.username).map {
            createdBoard: Board => Created(Json.toJson(createdBoard))
          }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /board/{id}
  def updateBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[BoardUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        boardUpdateRequest => {
          boardRepository.update(id, boardUpdateRequest, request.username)
            .map {
              case Right(_) => NoContent
              case Left(_: NotFoundException) => NotFound
              case Left(_: ForbiddenException) => Forbidden
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  def addMemberToBoard (id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        ControllerUtils.badRequest,
        memberAdd => {
          boardRepository.addMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  def deleteMemberFromBoard (id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        ControllerUtils.badRequest,
        memberAdd => {
          boardRepository.deleteMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  private def generateHypermediaBoardControls(board: Board) = {
      Future.successful(
        ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.BoardController.findBoardById(board.id).toString, "GET", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("deleteBoard", "Delete this board", routes.BoardController.deleteBoard(board.id).toString, "DELETE", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("updateLabel", "Update this board's label", routes.BoardController.updateBoard(board.id).toString, "PUT", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("addMemberToBoard", "Add a member to this board", routes.BoardController.addMemberToBoard(board.id).toString, "PUT", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("deleteMemberFromBoard", "Delete a member from this board", routes.BoardController.deleteMemberFromBoard(board.id).toString, "PUT", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("createList", "Create a new list in this board", routes.TasksListController.createNewListTask.toString, "POST", "application/json") :: List()
      )
  }

  private def generateHypermediaListsControls(lists: List[TasksList]) = {
    var listTasksList: List[JsObject] = List()
    for (tasksList <- lists)
      listTasksList = ControllerUtils.createIdAndLabelElementJsonLink(tasksList.id, tasksList.label, "get", routes.TasksListController.findListTaskById(tasksList.id).toString, "GET", "application/json") :: listTasksList
    Future.successful(listTasksList)
  }

  private def addBoardsHypermediaAndOk (listOfBoards: List[Board], username: String): Result = {
    val listSelfMethods: List[JsObject] =
      ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.BoardController.allUserBoards().toString, "GET", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("create", "Create a new board", routes.BoardController.createNewBoard.toString, "POST", "application/json") :: List()
    var listBoards: List[JsObject] = List()
    for (board <- listOfBoards)
      listBoards = ControllerUtils.createIdAndLabelElementJsonLink(board.id, board.label, "get", routes.BoardController.findBoardById(board.id).toString, "GET", "application/json") :: listBoards
    Ok(Json.obj("username" -> username, "boards" -> listBoards, "@controls" -> listSelfMethods))
  }

}