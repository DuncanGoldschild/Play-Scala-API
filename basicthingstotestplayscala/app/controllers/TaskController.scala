package controllers


import scala.concurrent._

import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton

import repositories.MongoTaskRepository

import models.{ForbiddenException, NotArchivedException, NotFoundException, TaskCreationRequest, TaskUpdateRequest}

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
                                  jwtService : JwtGenerator,
                                  controllerUtils: ControllerUtils,
                                  appAction: AppAction
                                ) extends AbstractController(components) {

  // Display the task by its id with GET /task/"id"
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

  // Delete with DELETE /task/"id"
  def deleteTask(id : String): Action[JsValue] = appAction.async(parse.json) { request =>
    taskRepository.delete(id, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
        case Left(_: NotArchivedException) => Locked
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /tasks
  def createNewTask: Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskCreationRequest]
    taskResult.fold(
        controllerUtils.badRequest,
      task => {
        taskRepository.createOne(task, "Pierrot").map {
          createdTask => Ok(Json.toJson(createdTask))
        }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }

  // Update with PUT /task/"id"
  def updateTask(id : String): Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskUpdateRequest]
    taskResult.fold(
        controllerUtils.badRequest,
      taskUpdateRequest => {
        taskRepository.update(id, taskUpdateRequest, request.username)
          .map {
            case Right(_) => NoContent
            case Left(_: NotFoundException) => NotFound
            case Left(_: ForbiddenException) => Forbidden
          }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }
}