package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.{BadRequestException, ForbiddenException, NotArchivedException, NotFoundException, Task, TaskCreationRequest, TaskUpdateRequest}



class MongoTaskRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi,
                                       tasksListRepository : MongoTasksListRepository
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository [Task] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("task"))

  def createOne(newTask: TaskCreationRequest, username: String): Future[Task] = {
    val insertedTask = Task(BSONObjectID.generate().stringify, newTask.label, newTask.description, newTask.archived, newTask.listId, Seq(username))
    collection.flatMap(_.insert.one(insertedTask)).map { _ => insertedTask }
  }


  def updateOne (id: String, newTask: TaskUpdateRequest): Future[Option[Unit]] = {
    val updatedTask = Task(id, newTask.label, newTask.description, newTask.archived, newTask.listId, newTask.membersUsername)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedTask, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

  def findOne(id: String): Future[Option[Task]] = {
    collection.flatMap(_.find(idSelector(id)).one[Task])
  }

  def archiveOne (id: String): Future[Option[Unit]]  = {
    collection.flatMap(_.findAndUpdate(
      idSelector(id),
      BSONDocument("$set" -> BSONDocument("archived" -> true)),
      fetchNewObject = true)
      .map{
        _.result[Task]
          .map {
            _ =>
          }
      }
    )
  }
  def archive(id: String, username : String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) => tasksListRepository.findOne(task.listId)
          .flatMap {
            case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) =>
              archiveOne(id)
                .map {
                  _ => Right()
                }
            case None => Future.successful(Left(BadRequestException()))
            case _ => Future.successful(Left(ForbiddenException()))
          }
        case None => Future.successful(Left(NotFoundException()))
      }
  }

  def create(newTask: TaskCreationRequest, username : String): Future[Either[Exception, Task]] = {
    tasksListRepository.findOne(newTask.listId)
      .flatMap {
        case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) =>
          createOne(newTask, username)
            .map {
              createdTask => Right(createdTask)
            }
        case None => Future.successful(Left(BadRequestException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def delete (taskId : String, username : String) : Future[Either[Exception, Unit]] = {
    findOne(taskId)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          if (task.archived) {
            deleteOne(taskId)
              .map {
                case Some(_) => Right()
              }
          } else Future.successful(Left(NotArchivedException()))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def find (taskId : String, username : String) : Future[Either[Exception, Task]] = {
    findOne(taskId)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) => Future.successful(Right(task))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def update (taskUpdateRequestId : String, taskUpdateRequest: TaskUpdateRequest, username : String): Future[Either[Exception, Unit]] = {
    tasksListRepository.findOne(taskUpdateRequest.listId)
      .flatMap {
        case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) =>
          findOne(taskUpdateRequestId)
            .flatMap {
              case Some(task: Task) if isUsernameContainedInTask(username, task) =>
                updateOne(taskUpdateRequestId, taskUpdateRequest)
                  .map {
                    case Some(_) => Right()
                  }
              case None => Future.successful(Left(NotFoundException()))
              case _ => Future.successful(Left(ForbiddenException()))
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }
  private def isUsernameContainedInTask (username: String, task: Task): Boolean = task.membersUsername.contains(username)

}