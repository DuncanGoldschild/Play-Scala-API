package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc.{Action, _}
import play.api.libs.json._
import com.google.inject.Singleton
import hypermedia.HypermediaControl.EmbeddedEntity
import hypermedia.{Hypermedia, HypermediaControl}
import metadata.Http
import repositories.{MongoTaskRepository, MongoTasksListRepository}
import models.{BadRequestException, ForbiddenException, MemberAddOrDelete, NotFoundException, Task, TasksList, TasksListCreationRequest, TasksListUpdateRequest}
import utils.{AuthenticatedAction, ControllerUtils, RequestWithAuth}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TasksListController @Inject()(
                                     components: ControllerComponents,
                                     listTaskRepository: MongoTasksListRepository,
                                     taskRepository: MongoTaskRepository,
                                     authenticatedAction: AuthenticatedAction
                                ) extends AbstractController(components) {

  // Display the ListTask by its id with GET /list/{id}
  def findListTaskById(id: String): Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    listTaskRepository.find(id, request.username)
      .zip(taskRepository.listAllTasksFromListId(id))
      .map {
        case (Right(list), tasks: List[Task]) =>
          Ok(Hypermedia.writeResponseDocument(
            list, generateHypermediaTasksControls(tasks), generateHypermediaListSelfControls(list)
          ))
        case (Left(_: NotFoundException), _) => NotFound
        case (Left(_: ForbiddenException), _) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Display all ListTask elements with GET /lists
  def allListTasks: Action[AnyContent] = authenticatedAction.async(parse.default) { request: RequestWithAuth[AnyContent] =>
    listTaskRepository.listAllFromUsername(request.username).map {
      list => Ok(Json.toJson(list))
    }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /list/{id}
  def deleteListTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request: RequestWithAuth[JsValue] =>
    listTaskRepository.delete(id, request.username)
      .map {
        case Right(_) => NoContent
        case Left(_: NotFoundException) => NotFound
        case Left(_: ForbiddenException) => Forbidden
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Add with POST /lists
  def createNewListTask: Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[TasksListCreationRequest]
      .fold(
        ControllerUtils.badRequest,
        listToCreate => {
          listTaskRepository.create(listToCreate, request.username).map {
            case Right(createdTasksList) =>
              Created(Hypermedia.writeResponseDocument(
                createdTasksList,
                generateHypermediaListSelfControls(createdTasksList)
              ))
            case Left(exception: ForbiddenException) => Forbidden(exception.message)
            case Left(exception: BadRequestException) => BadRequest(exception.message)
          }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /list/{id}
  def updateListTask(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[TasksListUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        listToUpdate => {
          listTaskRepository.update(id, listToUpdate, request.username)
            .map {
              case Right(_) => NoContent
              case Left(_: NotFoundException) => NotFound
              case Left(_: ForbiddenException) => Forbidden
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  def addMemberToList(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        ControllerUtils.badRequest,
        memberAdd => {
          listTaskRepository.addMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  def deleteMemberFromList(id: String): Action[JsValue] = authenticatedAction.async(parse.json) { request =>
    request.body.validate[MemberAddOrDelete]
      .fold(
        ControllerUtils.badRequest,
        memberAdd => {
          listTaskRepository.deleteMember(id, request.username, memberAdd.username)
            .map {
              case Right(_) => NoContent
              case Left(exception: NotFoundException) => NotFound(exception.message)
              case Left(exception: ForbiddenException) => Forbidden(exception.message)
              case Left(exception: BadRequestException) => BadRequest(exception.message)
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  private def generateHypermediaListSelfControls(list: TasksList): Map[String, HypermediaControl] = Map(
    Hypermedia.createControl("self", "Self informations", routes.TasksListController.findListTaskById(list.id), Http.MediaType.JSON),
    Hypermedia.createControl("deleteList", "Delete this list", routes.TasksListController.deleteListTask(list.id), Http.MediaType.JSON),
    Hypermedia.createControl("updateListLabel", "Update this list's label", routes.TasksListController.updateListTask(list.id), Http.MediaType.JSON, Schemas.updateLabelSchema),
    Hypermedia.createControl("addMemberToList", "Add a member to this list", routes.TasksListController.addMemberToList(list.id), Http.MediaType.JSON, Schemas.addDeleteMemberSchema),
    Hypermedia.createControl("deleteMemberFromList", "Delete a member from this list", routes.TasksListController.deleteMemberFromList(list.id), Http.MediaType.JSON, Schemas.addDeleteMemberSchema),
    Hypermedia.createControl("createTask", "Create a new task", routes.TaskController.createNewTask(), Http.MediaType.JSON, Schemas.createTaskSchema)
  )

  private def generateHypermediaTasksControls(listOfTask: List[Task]): List[EmbeddedEntity] =
    listOfTask map { task =>
      Hypermedia.createControl(task.id, task.label, "get", routes.TaskController.findTaskById(task.id), Http.MediaType.JSON)
    }

}