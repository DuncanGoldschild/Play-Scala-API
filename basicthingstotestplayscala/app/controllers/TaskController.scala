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
      }.recover(logAndInternalServerError)
  }

  // Display all Task elements with GET /tasks
  def allTasks: Action[AnyContent] = appAction.async {
    taskRepository.listAll.map {
      list => Ok(Json.toJson(list))
    }.recover(logAndInternalServerError)
  }

  // Delete with DELETE /task/"id"
  def deleteTask(id : String): Action[AnyContent] =  appAction.async {
    taskRepository.deleteOne(id)
      .map {
        case Some(_) => NoContent
        case None => NotFound
      }.recover(logAndInternalServerError)
  }

  // Add with POST /tasks
  def createNewTask: Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskCreationRequest]
    taskResult.fold(
      errors => {
        badRequest(errors)
      },
      task => {
        taskRepository.createOne(task, "Pierrot").map {
          createdTask => Ok(Json.toJson(createdTask))
        }.recover(logAndInternalServerError)
      }
    )
  }

  // Update with PUT /task/"id"
  def updateTask(id : String): Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskUpdateRequest]
    taskResult.fold(
      errors => {
        badRequest(errors)
      },
      task => {
        taskRepository.updateOne(id,task)
          .map {
            case Some(_) => NoContent
            case None => NotFound
          }.recover(logAndInternalServerError)
      }
    )
  }

  private def verifyTokenAndGetUsername(request : UserRequest[JsValue]): Option[String] = {
    request.headers.get("Authorization")
    match {
      case Some(token : String) => jwtService.getUsernameFromToken(token)
      case None => None
    }
  }

  private def badRequest (errors : Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] = {
    Future.successful(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(errors))))
  }

  private def logAndInternalServerError: PartialFunction[Throwable, Result] = {
    case e : Throwable =>
      logger.error(e.getMessage, e)
      InternalServerError

  }
}