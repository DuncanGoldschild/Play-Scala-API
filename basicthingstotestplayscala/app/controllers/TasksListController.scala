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
import repositories.MongoListTaskRepository
import models.{TasksListCreationRequest, TasksListUpdateRequest}
import utils.{AppAction, ControllerUtils}


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class TasksListController @Inject()(
                                  components: ControllerComponents,
                                  listTaskRepository: MongoListTaskRepository,
                                  controllerUtils: ControllerUtils,
                                  appAction: AppAction
                                ) extends AbstractController(components) {

  private val logger = Logger(this.getClass)

  // Display the ListTask by its id with GET /list/"id"
  def findListTaskById(id: String): Action[AnyContent] = appAction.async {
    listTaskRepository.findOne(id)
      .map {
        case Some(listTask) => Ok(Json.toJson(listTask))
        case None => NotFound
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Display all ListTask elements with GET /lists
  def allListTasks: Action[AnyContent] = appAction.async {
    listTaskRepository.listAll.map {
      list => Ok(Json.toJson(list))
    }.recover(controllerUtils.logAndInternalServerError)
  }

  // Delete with DELETE /list/"id"
  def deleteListTask(id : String): Action[AnyContent] =  appAction.async {
    listTaskRepository.deleteOne(id)
      .map {
        case Some(_) => NoContent
        case None => NotFound
      }.recover(controllerUtils.logAndInternalServerError)
  }

  // Add with POST /lists
  def createNewListTask: Action[JsValue] = appAction.async(parse.json) { request =>
    val listTaskResult = request.body.validate[TasksListCreationRequest]
    listTaskResult.fold(
      controllerUtils.badRequest,
      listToCreate => {
        listTaskRepository.createOne(listToCreate, request.username).map {
          createdListTask => Ok(Json.toJson(createdListTask))
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
        listTaskRepository.updateOne(id,listToUpdate)
          .map {
            case Some(_) => NoContent
            case None => NotFound
          }.recover(controllerUtils.logAndInternalServerError)
      }
    )
  }
}