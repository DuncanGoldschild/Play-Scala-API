package controllers


import scala.concurrent._
import scala.collection.Seq
import javax.inject._

import ExecutionContext.Implicits.global
import reactivemongo.play.json._
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger
import com.google.inject.Singleton
import repositories.MongoTaskRepository
import models.{TaskCreationRequest, TaskUpdateRequest}
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

  private val logger = Logger(this.getClass)

  // Display the task by its id with GET /task/"id"
  def findTaskById(id: String): Action[AnyContent] = appAction.async {
    taskRepository.findOne(id)
      .map {
        case Some(task) => Ok(Json.toJson(task))
        case None => NotFound
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Display all Task elements with GET /tasks
  def allTasks: Action[AnyContent] = appAction.async {
    taskRepository.listAll.map {
      list => Ok(Json.toJson(list))
    }.recover(controllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /task/"id"
  def deleteTask(id : String): Action[AnyContent] =  appAction.async {
    taskRepository.deleteOne(id)
      .map {
        case Some(_) => NoContent
        case None => NotFound
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
      task => {
        taskRepository.updateOne(id,task)
          .map {
            case Some(_) => NoContent
            case None => NotFound
          }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }
}