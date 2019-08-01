package controllers


import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.MongoTaskRepository
import models.{BadRequestException, ForbiddenException, TaskNotArchivedException, NotFoundException, TaskCreationRequest, TaskUpdateRequest}
import services.JwtGenerator
import utils.{AppAction, ControllerUtils, UserRequest}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TaskController @Inject() (
                                  components: ControllerComponents,
                                  taskRepository: MongoTaskRepository,
                                  appAction: AppAction
                                ) extends AbstractController(components) {

  val controllerUtils = new ControllerUtils(components)

  // Display the task by its id with GET /task/{id}
  def findTaskById(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    taskRepository.find(id, request.username)
      .map {
        case Right(task) => Ok(Json.toJson(task))
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Display all Task elements with GET /tasks
  def allTasks: Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    taskRepository.listAllFromUsername(request.username)
      .map {
        list => Ok(Json.toJson(list))
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /task/{id}
  def deleteTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    taskRepository.delete(id, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
        case Left(_: TaskNotArchivedException) => Locked
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /tasks
  def createNewTask: Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskCreationRequest]
    taskResult.fold(
        controllerUtils.badRequest,
      newTask => {
        taskRepository.create(newTask, request.username).map {
          case Right (createdTasksList) => Created(Json.toJson(createdTasksList))
          case Left (_: ForbiddenException) => Forbidden("You don't have access to this List")
          case Left (_: BadRequestException) => BadRequest("List does not exist")
        }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }
  //Archive with PUT /task/{id}/archive
  def archiveTask(id: String, archiveOrRestore: Boolean): Action[JsValue] = appAction.async(parse.json) { request =>
        taskRepository.archive(id, request.username, archiveOrRestore)
          .map {
            case Right(_) => NoContent
            case Left(exception: NotFoundException) => NotFound(exception.message)
            case Left(exception: ForbiddenException) => Forbidden(exception.message)
          }.recover(controllerUtils.logAndInternalServerError)
      }

  // Update with PUT /task/{id}
  def updateTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskUpdateRequest]
    taskResult.fold(
        controllerUtils.badRequest,
      taskUpdateRequest => {
        taskRepository.update(id, taskUpdateRequest, request.username)
          .map {
            case Right(_) => NoContent
            case Left(exception: NotFoundException) => NotFound(exception.message)
            case Left(exception: ForbiddenException) => Forbidden(exception.message)
            case Left(exception: BadRequestException) => BadRequest(exception.message)
          }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }
}