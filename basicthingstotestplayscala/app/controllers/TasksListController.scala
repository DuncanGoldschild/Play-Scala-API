package controllers


import scala.concurrent._
import scala.collection.Seq
import javax.inject._

import ExecutionContext.Implicits.global
import reactivemongo.play.json._
import play.api.mvc.{Action, _}
import play.api.libs.json._
import play.api.Logger
import com.google.inject.Singleton
import repositories.MongoTasksListRepository
import models.{BadRequestException, ForbiddenException, NotFoundException, TasksListCreationRequest, TasksListUpdateRequest}
import utils.{AppAction, ControllerUtils, UserRequest}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TasksListController @Inject()(
                                     components: ControllerComponents,
                                     listTaskRepository: MongoTasksListRepository,
                                     controllerUtils: ControllerUtils,
                                     appAction: AppAction
                                ) extends AbstractController(components) {

  // Display the ListTask by its id with GET /list/"id"
  def findListTaskById(id: String): Action[JsValue] = appAction.async(parse.json) { request : UserRequest[JsValue] =>
    listTaskRepository.find(id, request.username)
      .map {
        case Right(list) => Ok(Json.toJson(list))
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Display all ListTask elements with GET /lists
  def allListTasks: Action[AnyContent] = appAction.async { request =>
    listTaskRepository.listAllFromUsername(request.username).map {
      list => Ok(Json.toJson(list))
    }.recover(controllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /list/"id"
  def deleteListTask(id : String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
  listTaskRepository.delete(id, request.username)
    .map {
      case Right(_) => NoContent
      case Left(_: NotFoundException) => NotFound
      case Left(_: ForbiddenException) => Forbidden
    }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /lists
  def createNewListTask: Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[TasksListCreationRequest]
      .fold(
        controllerUtils.badRequest,
        listToCreate => {
          listTaskRepository.create(listToCreate, request.username).map {
            case Right (createdTasksList) => Ok(Json.toJson(createdTasksList))
            case Left (_: ForbiddenException) => Forbidden("You dont have access to this board")
            case Left (_: BadRequestException) => BadRequest("Board does not exist")
          }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /list/"id"
  def updateListTask(id : String): Action[JsValue] = appAction.async(parse.json) { request =>
    val listTaskResult = request.body.validate[TasksListUpdateRequest]
    listTaskResult.fold(
      controllerUtils.badRequest,
      listToUpdate => {
        listTaskRepository.update(id,listToUpdate, request.username)
          .map {
            case Right(_) => NoContent
            case Left(_: NotFoundException) => NotFound
            case Left(_: ForbiddenException) => Forbidden
          }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }
}