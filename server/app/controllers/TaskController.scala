package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import hypermedia.{Hypermedia, HypermediaControl}
import metadata.Http
import repositories.MongoTaskRepository
import models.{ArchiveOrRestoreRequest, BadRequestException, DescriptionUpdateRequest, ForbiddenException, LabelUpdateRequest, ListIdOfTaskUpdateRequest, MemberAddOrDelete, NotFoundException, Task, TaskCreationRequest, TaskNotArchivedException, TaskUpdateRequest}
import utils.{AuthenticatedAction, ControllerUtils, RequestWithAuth}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TaskController @Inject() (
                                 components: ControllerComponents,
                                 taskRepository: MongoTaskRepository,
                                 authenticatedAction: AuthenticatedAction
                                ) extends AbstractController(components) {

  // Display the task by its id with GET /task/{id}
  def findTaskById(id: String): Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    taskRepository.find(id, request.username)
      .map {
        case Right(task) =>
          Ok(Hypermedia.writeResponseDocument(
            task, generateHypermediaTaskSelf(task)
          ))
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Display all Task elements with GET /tasks
  def allTasks: Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    taskRepository.listAllFromUsername(request.username)
      .map {
        list => Ok(Json.toJson(list))
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /task/{id}
  def deleteTask(id: String): Action[AnyContent] = authenticatedAction.async(parse.default) {
    { request =>
      taskRepository.delete(id, request.username)
        .map {
          case Right(_) => NoContent
          case Left(_: NotFoundException) => NotFound
          case Left(_: ForbiddenException) => Forbidden
          case Left(_: TaskNotArchivedException) => Locked
        }.recover(ControllerUtils.logAndInternalServerError)
    }
  }

  // Add with POST /tasks
  def createNewTask: Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskCreationRequest]
    taskResult.fold(
      ControllerUtils.badRequest,
      newTask => {
        taskRepository.create(newTask, request.username).map {
          case Right(createdTask) =>
            Created(Hypermedia.writeResponseDocument(
              createdTask, generateHypermediaTaskSelf(createdTask)
            ))
          case Left(_: ForbiddenException) => Forbidden("You don't have access to this List")
          case Left(_: BadRequestException) => BadRequest("List does not exist")
        }.recover(ControllerUtils.logAndInternalServerError)
      }
    )
  }

  def addMemberToTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        ControllerUtils.badRequest,
        memberAdd => {
          taskRepository.addMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  def deleteMemberFromTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        ControllerUtils.badRequest,
        memberAdd => {
          taskRepository.deleteMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  //Archive with PUT /task/{id}/archive
  def archiveTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[ArchiveOrRestoreRequest]
        .fold(
          ControllerUtils.badRequest,
          booleanArchiveOrRestore => {
            taskRepository.archive(id, request.username, booleanArchiveOrRestore.archive)
              .map {
                case Right(_) => NoContent
                case Left(exception: NotFoundException) => NotFound(exception.message)
                case Left(exception: ForbiddenException) => Forbidden(exception.message)
              }.recover(ControllerUtils.logAndInternalServerError)
          }
        )

  }

  def changeParentListOfTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[ListIdOfTaskUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        listIdUpdateRequest =>
          taskRepository.changeList(id, request.username, listIdUpdateRequest.listId)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
      )
  }

  def changeLabelOfTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[LabelUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        newLabelRequest =>
          taskRepository.changeLabel(id, request.username, newLabelRequest.label)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
      )
  }

  def changeDescriptionOfTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[DescriptionUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        newDescriptionRequest =>
          taskRepository.changeDescription(id, request.username, newDescriptionRequest.description)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
      )
  }

  // Update with PUT /task/{id}
  def updateTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskUpdateRequest]
    taskResult.fold(
      ControllerUtils.badRequest,
      taskUpdateRequest => {
        taskRepository.update(id, taskUpdateRequest, request.username)
          .map {
            case Right(_) => NoContent
            case Left(exception: NotFoundException) => NotFound(exception.message)
            case Left(exception: ForbiddenException) => Forbidden(exception.message)
            case Left(exception: BadRequestException) => BadRequest(exception.message)
          }.recover(ControllerUtils.logAndInternalServerError)
      }
    )
  }

  private def generateHypermediaTaskSelf(task: Task): Map[String, HypermediaControl] = Map(
    Hypermedia.createControl("self", "Self informations", routes.TaskController.findTaskById(task.id), Http.MediaType.JSON),
    Hypermedia.createControl("deleteTask", "Delete this task", routes.TaskController.deleteTask(task.id), Http.MediaType.JSON),
    Hypermedia.createControl("updateTask", "Update this task", routes.TaskController.updateTask(task.id), Http.MediaType.JSON, Schemas.updateTaskSchema),
    Hypermedia.createControl("archiveTask", "Archive this task with true", routes.TaskController.archiveTask(task.id), Http.MediaType.JSON, Schemas.archiveOrRestoreSchema),
    Hypermedia.createControl("restoreTask", "Restore this task with false", routes.TaskController.archiveTask(task.id), Http.MediaType.JSON, Schemas.archiveOrRestoreSchema),
    Hypermedia.createControl("updateListId", "Update the listId of this task", routes.TaskController.changeParentListOfTask(task.id), Http.MediaType.JSON, Schemas.updateTaskSchema),
    Hypermedia.createControl("updateLabel", "Update the label of this task", routes.TaskController.changeLabelOfTask(task.id), Http.MediaType.JSON, Schemas.updateLabelSchema),
    Hypermedia.createControl("updateDescription", "Change description of this task", routes.TaskController.changeDescriptionOfTask(task.id), Http.MediaType.JSON, Schemas.archiveOrRestoreSchema),
    Hypermedia.createControl("addMemberToTask", "Add a member to this task", routes.TaskController.addMemberToTask(task.id), Http.MediaType.JSON, Schemas.addDeleteMemberSchema),
    Hypermedia.createControl("deleteMemberFromTask", "Delete a member from this task", routes.TaskController.deleteMemberFromTask(task.id), Http.MediaType.JSON, Schemas.addDeleteMemberSchema)
  )

}