package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.MongoTaskRepository
import models.{BadRequestException, DescriptionUpdateRequest, ForbiddenException, LabelUpdateRequest, ListIdOfTaskUpdateRequest, MemberAddOrDelete, NotFoundException, Task, TaskCreationRequest, TaskNotArchivedException, TaskUpdateRequest}
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
        case Right(task) =>
          addHypermediaToTaskAndOk(request.username, task)
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
          case Right(createdTasksList) => Created(Json.toJson(createdTasksList))
          case Left(_: ForbiddenException) => Forbidden("You don't have access to this List")
          case Left(_: BadRequestException) => BadRequest("List does not exist")
        }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }

  def addMemberToTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        controllerUtils.badRequest,
        memberAdd => {
          taskRepository.addMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(controllerUtils.logAndInternalServerError)
        }
      )
  }

  def deleteMemberFromTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        controllerUtils.badRequest,
        memberAdd => {
          taskRepository.deleteMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
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

  def changeParentListOfTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[ListIdOfTaskUpdateRequest]
      .fold(
        controllerUtils.badRequest,
        listIdUpdateRequest =>
          taskRepository.changeList(id, request.username, listIdUpdateRequest.listId)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
            }.recover(controllerUtils.logAndInternalServerError)
      )
  }

  def changeLabelOfTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[LabelUpdateRequest]
      .fold(
        controllerUtils.badRequest,
        newLabelRequest =>
          taskRepository.changeLabel(id, request.username, newLabelRequest.label)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
            }.recover(controllerUtils.logAndInternalServerError)
      )
  }

  def changeDescriptionOfTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[DescriptionUpdateRequest]
      .fold(
        controllerUtils.badRequest,
        newDescriptionRequest =>
          taskRepository.changeDescription(id, request.username, newDescriptionRequest.description)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
            }.recover(controllerUtils.logAndInternalServerError)
      )
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

  private def addHypermediaToTaskAndOk(username: String, task: Task): Result = {
    val listSelfMethods: List[JsObject] =
      controllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.TaskController.findTaskById(task.id).toString, "GET", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("deleteTask", "Delete this task", routes.TaskController.deleteTask(task.id).toString, "DELETE", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("updateTask", "Update this task", routes.TaskController.updateTask(task.id).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("archiveTask", "Archive this task", routes.TaskController.archiveTask(task.id, true).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("restoreTask", "Restore this task", routes.TaskController.archiveTask(task.id, false).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("updateListId", "Update the listId of this task", routes.TaskController.changeParentListOfTask(task.id).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("updateLabel", "Update the label of this task", routes.TaskController.changeLabelOfTask(task.id).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("updateDescription", "Change description of this task", routes.TaskController.changeDescriptionOfTask(task.id).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("addMemberToTask", "Add a member to this task", routes.TaskController.addMemberToTask(task.id).toString, "PUT", "application/json") ::
        controllerUtils.createCRUDActionJsonLink("deleteMemberFromTask", "Delete a member from this task", routes.TaskController.deleteMemberFromTask(task.id).toString, "PUT", "application/json") :: List()
    Ok(Json.obj("info" -> Json.toJson(task), "@controls" -> listSelfMethods))
  }
}