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

  def test: Action[AnyContent] = Action.async {
    for {
      welcomeControls <- generateWelcomeHypermedia
    } yield Ok(Json.obj( "@controls" -> welcomeControls))
  }

  // Returns a JSON of the board by its id with GET /board/{id}
  def findBoardById(id: String): Action[AnyContent] = appAction.async(parse.default) { request: UserRequest[AnyContent] =>
    boardRepository.find(id, request.username)
      .flatMap {
        case Right(board) =>
          for {
            lists <- tasksListRepository.listAllListsFromBoardId(id)
            listsWithControls <- generateHypermediaListsControls(lists)
            boardControls <- generateHypermediaBoardSelfControls(board)
          } yield Ok(ControllerUtils.hypermediaStructureResponse(Json.toJson(board), "lists", listsWithControls, boardControls))
        case Left(_: NotFoundException) => Future.successful(NotFound)
        case Left(_: ForbiddenException) => Future.successful(Forbidden)
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Returns a JSON of all board elements with GET /boards
  def allUserBoards: Action[AnyContent] = appAction.async(parse.default) { request: UserRequest[AnyContent] =>
    val username = request.username
    boardRepository.listAllFromUsername(username)
      .flatMap {
        listOfBoards =>
          for {
            boardsSelfControls <- generateHypermediaBoardsSelfControls
            boardsControls <- generateHypermediaBoardsControls(listOfBoards)
          } yield Ok(ControllerUtils.hypermediaStructureResponse(Json.obj("username" -> username), "boards", boardsControls, boardsSelfControls))
      }
  }

  // Delete with DELETE /board/{id}
  def deleteBoard(id: String): Action[AnyContent] = appAction.async(parse.default) { request: UserRequest[AnyContent] =>
    boardRepository.delete(id, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    request.body.validate[BoardCreationRequest]
      .fold(
        ControllerUtils.badRequest,
        board => {
          boardRepository.createOne(board, request.username).flatMap {
            createdBoard: Board =>
              for {
                boardWithControls <- generateHypermediaBoardSelfControls(createdBoard)
              } yield Created(Json.obj("info" -> Json.toJson(createdBoard), "@controls" -> boardWithControls))
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

  def addMemberToBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  def deleteMemberFromBoard(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  private def generateWelcomeHypermedia: Future[List[JsObject]] = {
    Future.successful(
      ControllerUtils.createCRUDActionJsonLink("auth", "Self informations", routes.MemberController.authMember.toString, "POST", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("createMember", "Create a new account", routes.MemberController.createNewMember.toString, "POST", "application/json") :: List()
    )
  }

  private def generateHypermediaBoardSelfControls(board: Board): Future[List[JsObject]] = {
    Future.successful(
      ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.BoardController.findBoardById(board.id).toString, "GET", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("deleteBoard", "Delete this board", routes.BoardController.deleteBoard(board.id).toString, "DELETE", "application/json") ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("updateLabel", "Update this board's label", routes.BoardController.updateBoard(board.id).toString, "PUT", "application/json", routes.Schemas.updateLabelSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("addMemberToBoard", "Add a member to this board", routes.BoardController.addMemberToBoard(board.id).toString, "PUT", "application/json", routes.Schemas.addDeleteMemberSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("deleteMemberFromBoard", "Delete a member from this board", routes.BoardController.deleteMemberFromBoard(board.id).toString, "PUT", "application/json", routes.Schemas.addDeleteMemberSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("createList", "Create a new list", routes.TasksListController.createNewListTask.toString, "POST", "application/json", routes.Schemas.createListSchema.toString) :: List()
    )
  }

  private def generateHypermediaListsControls(lists: List[TasksList]): Future[List[JsObject]] = {
    var listTasksList: List[JsObject] = List()
    for (tasksList <- lists)
      listTasksList = ControllerUtils.createIdAndLabelElementJsonLink(tasksList.id, tasksList.label, "get", routes.TasksListController.findListTaskById(tasksList.id).toString, "GET", "application/json") :: listTasksList
    Future.successful(listTasksList)
  }

  private def generateHypermediaBoardsSelfControls: Future[List[JsObject]] = {
    Future.successful(
      ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.BoardController.allUserBoards().toString, "GET", "application/json") ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("create", "Create a new board", routes.BoardController.createNewBoard.toString, "POST", "application/json",routes.Schemas.createBoardSchema.toString) :: List()
    )
  }

  private def generateHypermediaBoardsControls(listOfBoards: List[Board]): Future[List[JsObject]] = {
    var listBoards: List[JsObject] = List()
    for (board <- listOfBoards)
      listBoards = ControllerUtils.createIdAndLabelElementJsonLink(board.id, board.label, "get", routes.BoardController.findBoardById(board.id).toString, "GET", "application/json") :: listBoards
    Future.successful(listBoards)
  }
}