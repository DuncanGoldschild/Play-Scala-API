package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.MongoTaskRepository
import models.{ArchiveOrRestoreRequest, BadRequestException, DescriptionUpdateRequest, ForbiddenException, LabelUpdateRequest, ListIdOfTaskUpdateRequest, MemberAddOrDelete, NotFoundException, Task, TaskCreationRequest, TaskNotArchivedException, TaskUpdateRequest}
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

  // Display the task by its id with GET /task/{id}
  def findTaskById(id: String): Action[AnyContent] = appAction.async(parse.default) { request: UserRequest[AnyContent] =>
    taskRepository.find(id, request.username)
      .flatMap {
        case Right(task) =>
          for {
            taskControls <- generateHypermediaTaskSelf(task)
          } yield Ok(Json.obj("info" -> Json.toJson(task), "@controls" -> taskControls))
        case Left(_: NotFoundException) => Future.successful(NotFound)
        case Left(_: ForbiddenException) => Future.successful(Forbidden)
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Display all Task elements with GET /tasks
  def allTasks: Action[AnyContent] = appAction.async(parse.default) { request: UserRequest[AnyContent] =>
    taskRepository.listAllFromUsername(request.username)
      .map {
        list => Ok(Json.toJson(list))
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /task/{id}
  def deleteTask(id: String): Action[AnyContent] = appAction.async(parse.default) {
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
  def createNewTask: Action[JsValue] = appAction.async(parse.json) { request =>
    val taskResult = request.body.validate[TaskCreationRequest]
    taskResult.fold(
      ControllerUtils.badRequest,
      newTask => {
        taskRepository.create(newTask, request.username).flatMap {
          case Right(createdTask) =>
            for {
              taskWithControls <- generateHypermediaTaskSelf(createdTask)
            } yield Created(Json.obj("info" -> Json.toJson(createdTask), "@controls" -> taskWithControls))
          case Left(_: ForbiddenException) => Future.successful(Forbidden("You don't have access to this List"))
          case Left(_: BadRequestException) => Future.successful(BadRequest("List does not exist"))
        }.recover(ControllerUtils.logAndInternalServerError)
      }
    )
  }

  def addMemberToTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  def deleteMemberFromTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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
  def archiveTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  def changeParentListOfTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  def changeLabelOfTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  def changeDescriptionOfTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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
  def updateTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  private def generateHypermediaTaskSelf(task: Task): Future[List[JsObject]] = {
    Future.successful(
      ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.TaskController.findTaskById(task.id).toString, "GET", "application/json") ::
        ControllerUtils.createCRUDActionJsonLink("deleteTask", "Delete this task", routes.TaskController.deleteTask(task.id).toString, "DELETE", "application/json") ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("updateTask", "Update this task", routes.TaskController.updateTask(task.id).toString, "PUT", "application/json", routes.Schemas.updateTaskSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("archiveTask", "Archive this task with true", routes.TaskController.archiveTask(task.id).toString, "PUT", "application/json", routes.Schemas.archiveOrRestoreSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("restoreTask", "Restore this task with false", routes.TaskController.archiveTask(task.id).toString, "PUT", "application/json", routes.Schemas.archiveOrRestoreSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("updateListId", "Update the listId of this task", routes.TaskController.changeParentListOfTask(task.id).toString, "PUT", "application/json", routes.Schemas.listIdOfTaskUpdateSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("updateLabel", "Update the label of this task", routes.TaskController.changeLabelOfTask(task.id).toString, "PUT", "application/json", routes.Schemas.updateLabelSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("updateDescription", "Change description of this task", routes.TaskController.changeDescriptionOfTask(task.id).toString, "PUT", "application/json", routes.Schemas.descriptionUpdateSchema.toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("addMemberToTask", "Add a member to this task", routes.TaskController.addMemberToTask(task.id).toString, "PUT", "application/json", routes.Schemas.addDeleteMemberSchema().toString) ::
        ControllerUtils.createCRUDActionJsonLinkWithSchema("deleteMemberFromTask", "Delete a member from this task", routes.TaskController.deleteMemberFromTask(task.id).toString, "PUT", "application/json", routes.Schemas.addDeleteMemberSchema.toString) :: List()
    )
  }
}