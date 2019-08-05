package controllers


import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import hypermedia.BoardLink.linkTo
import hypermedia.{Links}
import repositories.{MongoBoardRepository, MongoMemberRepository, MongoTasksListRepository}
import models.{Board, BoardCreationRequest, BoardUpdateRequest, ForbiddenException, NotFoundException}
import utils.{AppAction, ControllerUtils, UserRequest}

import scala.util.Success


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
          val links = Links()
          links.add(linkTo(routes.BoardController.findBoardById(id)).withSelfRel.withJsonMediaType)
          links.add(linkTo(routes.BoardController.deleteBoard(id)).withDeleteRel.withJsonMediaType)
          addListsRoutesAndOk(id, links, board)
        case Left(_: NotFoundException) => Future.successful(NotFound)
        case Left(_: ForbiddenException) => Future.successful(Forbidden)
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Returns a JSON of all board elements with GET /boards
  def allUserBoards: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    val username = request.username
    boardRepository.listAllFromUsername(username)
      .map {
        listOfBoards =>
          val links = Links()
          links.add(linkTo(routes.BoardController.allUserBoards()).withSelfRel.withJsonMediaType.withDisplayFormat("MethodOnSelf"))
          links.add(linkTo(routes.BoardController.createNewBoard).withRel("create", "POST").withJsonMediaType.withDisplayFormat("MethodOnSelf"))
          for (board <- listOfBoards) links.add(linkTo(routes.BoardController.findBoardById(board.id)).withGetRel.withId(board.id).withLabel(board.label).withJsonMediaType.withDisplayFormat("GetOneElement"))
          Ok(Json.toJson(links.addAsJsonTo(Json.obj("username" -> username))))
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
  private def addListsRoutesAndOk(id: String, links: Links, board: Board): Future[Result] =
    tasksListRepository.listAllListsFromBoardId(id)
      .map {
        listOfBoardLists =>
          for (listTask <- listOfBoardLists) links.add(linkTo(routes.TasksListController.findListTaskById(listTask.id)).withGetRel.withJsonMediaType.withDisplayFormat("GetOneElement").withId(listTask.id).withLabel(listTask.label))
          Ok(Json.toJson(links.addAsJsonTo(board)))
      }
}