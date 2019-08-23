package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import hypermedia.{Hypermedia, HypermediaControl}
import metadata.Http
import repositories.{MongoBoardRepository, MongoMemberRepository, MongoTasksListRepository}
import models.{BadRequestException, Board, BoardCreationRequest, BoardUpdateRequest, ForbiddenException, MemberAddOrDelete, NotFoundException, TasksList, User}
import utils.{AuthenticatedAction, ControllerUtils, RequestWithAuth}



@Singleton
class BoardController @Inject() (
                                  components: ControllerComponents,
                                  boardRepository: MongoBoardRepository,
                                  memberRepository: MongoMemberRepository,
                                  tasksListRepository: MongoTasksListRepository,
                                  authenticatedAction: AuthenticatedAction
                               ) extends AbstractController(components) {


  def findBoardById(id: String): Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    boardRepository.find(id, request.username)
      .zip(tasksListRepository.listAllListsFromBoardId(id))
      .map {
        case (Right(board), lists: List[TasksList]) =>
          Ok(Hypermedia.writeResponseDocument(
            board,
            generateHypermediaListsControls(lists),
            generateHypermediaBoardSelfControls(board)
          ))
        case (Left(_: NotFoundException), _) => NotFound
        case (Left(_: ForbiddenException), _) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  def allUserBoards: Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    val username = request.username
    boardRepository.listAllFromUsername(username)
      .map {
        listOfBoards =>
          Ok(Hypermedia.writeResponseDocument(
            User(username),
            generateHypermediaBoardsControls(listOfBoards),
            generateHypermediaBoardsSelfControls
          ))
      }
  }

  // Delete with DELETE /board/{id}
  def deleteBoard(id: String): Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    boardRepository.delete(id, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = authenticatedAction.async(parse.json) { request: RequestWithAuth[JsValue] =>
    request.body.validate[BoardCreationRequest]
      .fold(
        ControllerUtils.badRequest,
        board => {
          boardRepository.createOne(board, request.username).map {
            createdBoard: Board =>
              Created(Hypermedia.writeResponseDocument(
                createdBoard, generateHypermediaBoardSelfControls(createdBoard)
              ))
          }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /board/{id}
  def updateBoard(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
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

  def addMemberToBoard(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
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

  def deleteMemberFromBoard(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
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

  private def generateHypermediaBoardSelfControls(board: Board): Map[String, HypermediaControl] = Map(
    Hypermedia.createControl("self", "Self information", routes.BoardController.findBoardById(board.id), Http.MediaType.JSON),
    Hypermedia.createControl("deleteBoard", "Delete this board", routes.BoardController.deleteBoard(board.id), Http.MediaType.JSON),
    Hypermedia.createControl("updateLabel", "Update this board's label", routes.BoardController.updateBoard(board.id), Http.MediaType.JSON, Schemas.updateLabelSchema),
    Hypermedia.createControl("addMemberToBoard", "Add a member to this board", routes.BoardController.addMemberToBoard(board.id), Http.MediaType.JSON, Schemas.addDeleteMemberSchema),
    Hypermedia.createControl("deleteMemberFromBoard", "Delete a member from this board", routes.BoardController.deleteMemberFromBoard(board.id), Http.MediaType.JSON, Schemas.addDeleteMemberSchema),
    Hypermedia.createControl("createList", "Create a new list", routes.TasksListController.createNewListTask(), Http.MediaType.JSON, Schemas.createListSchema)
  )

  private def generateHypermediaListsControls(lists: List[TasksList]): List[HypermediaControl.EmbeddedEntity] =
    lists map {
      tasksList =>
        Hypermedia.createControl(
          tasksList.id,
          tasksList.label,
          "get",
          routes.TasksListController.findListTaskById(tasksList.id),
          Http.MediaType.JSON
        )
    }

  private def generateHypermediaBoardsSelfControls: Map[String, HypermediaControl] = Map(
    Hypermedia.createControl("self", "Self information", routes.BoardController.allUserBoards(), Http.MediaType.JSON),
    Hypermedia.createControl("create", "Create a new board", routes.BoardController.createNewBoard(), Http.MediaType.JSON, Schemas.createBoardSchema)
  )

  private def generateHypermediaBoardsControls(listOfBoards: List[Board]): List[HypermediaControl.EmbeddedEntity] =
    listOfBoards map {
      board =>
        Hypermedia.createControl(
          board.id,
          board.label,
          "get",
          routes.BoardController.findBoardById(board.id),
          Http.MediaType.JSON
        )
    }

}