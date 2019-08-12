package controllers

import scala.concurrent._
import javax.inject._

import ExecutionContext.Implicits.global
import play.api.mvc.{Action, _}
import play.api.libs.json._
import com.google.inject.Singleton
import repositories.{MongoTaskRepository, MongoTasksListRepository}
import models.{BadRequestException, ForbiddenException, MemberAddOrDelete, NotFoundException, TasksList, TasksListCreationRequest, TasksListUpdateRequest}
import utils.{AppAction, ControllerUtils, UserRequest}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TasksListController @Inject()(
                                     components: ControllerComponents,
                                     listTaskRepository: MongoTasksListRepository,
                                     taskRepository: MongoTaskRepository,
                                     appAction: AppAction
                                ) extends AbstractController(components) {

  // Display the ListTask by its id with GET /list/{id}
  def findListTaskById(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
    listTaskRepository.find(id, request.username)
      .flatMap {
        case Right(list) =>
          addHypermediaToListAndOk(request.username, list)
        case Left(_: NotFoundException) => Future.successful(NotFound)
        case Left(_: ForbiddenException) => Future.successful(Forbidden)
      }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Display all ListTask elements with GET /lists
  def allListTasks: Action[AnyContent] = appAction.async { request =>
    listTaskRepository.listAllFromUsername(request.username).map {
      list => Ok(Json.toJson(list))
    }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /list/{id}
  def deleteListTask(id: String): Action[JsValue] = appAction.async(parse.json) { request: UserRequest[JsValue] =>
  listTaskRepository.delete(id, request.username)
    .map {
      case Right(_) => NoContent
      case Left(_: NotFoundException) => NotFound
      case Left(_: ForbiddenException) => Forbidden
    }.recover(ControllerUtils.logAndInternalServerError)
  }

  // Add with POST /lists
  def createNewListTask: Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[TasksListCreationRequest]
      .fold(
        ControllerUtils.badRequest,
        listToCreate => {
          listTaskRepository.create(listToCreate, request.username).map {
            case Right (createdTasksList) => Created(Json.toJson(createdTasksList))
            case Left (exception: ForbiddenException) => Forbidden(exception.message)
            case Left (exception: BadRequestException) => BadRequest(exception.message)
          }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  // Update with PUT /list/{id}
  def updateListTask(id: String): Action[JsValue] = appAction.async(parse.json) { request =>
    request.body.validate[TasksListUpdateRequest]
      .fold(
        ControllerUtils.badRequest,
        listToUpdate => {
          listTaskRepository.update(id,listToUpdate, request.username)
            .map {
              case Right(_) => NoContent
              case Left(_: NotFoundException) => NotFound
              case Left(_: ForbiddenException) => Forbidden
            }.recover(ControllerUtils.logAndInternalServerError)
        }
      )
  }

  def addMemberToList (id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  def deleteMemberFromList (id: String): Action[JsValue] = appAction.async(parse.json) { request =>
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

  // Returns a list of all the lists contained in this board
  private def addHypermediaToListAndOk(username: String, list: TasksList): Future[Result] =
    taskRepository.listAllTasksFromListId(list.id)
      .map {
        listOfTasks =>
          val listSelfMethods: List[JsObject] =
            ControllerUtils.createCRUDActionJsonLink("self", "Self informations", routes.TasksListController.findListTaskById(list.id).toString, "GET", "application/json") ::
              ControllerUtils.createCRUDActionJsonLink("deleteList", "Delete this list", routes.TasksListController.deleteListTask(list.id).toString, "DELETE", "application/json") ::
              ControllerUtils.createCRUDActionJsonLink("updateListLabel", "Update this list's label", routes.TasksListController.updateListTask(list.id).toString, "PUT", "application/json") ::
              ControllerUtils.createCRUDActionJsonLink("addMemberToList", "Add a member to this list", routes.TasksListController.addMemberToList(list.id).toString, "PUT", "application/json") ::
              ControllerUtils.createCRUDActionJsonLink("deleteMemberFromList", "Delete a member from this list", routes.TasksListController.deleteMemberFromList(list.id).toString, "PUT", "application/json") ::
              ControllerUtils.createCRUDActionJsonLink("createTask", "Create a new task in this list", routes.TaskController.createNewTask.toString, "POST", "application/json") :: List()
          var listTasksList: List[JsObject] = List()
          for (task <- listOfTasks)
            listTasksList = ControllerUtils.createIdAndLabelElementJsonLink(task.id, task.label, "get", routes.TasksListController.findListTaskById(task.id).toString, "GET", "application/json") :: listTasksList
          Ok(Json.obj("info" -> Json.toJson(list), "tasks" -> listTasksList, "@controls" -> listSelfMethods))
      }

}