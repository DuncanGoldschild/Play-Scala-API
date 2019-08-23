package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.{BadRequestException, ForbiddenException, TaskNotArchivedException, NotFoundException, Task, TaskCreationRequest, TaskUpdateRequest}



class MongoTaskRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi,
                                       boardRepository: MongoBoardRepository,
                                       tasksListRepository: MongoTasksListRepository,
                                       memberRepository: MongoMemberRepository
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository [Task] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("task"))

  def createOne(newTask: TaskCreationRequest, username: String): Future[Task] = {
    val insertedTask = Task(BSONObjectID.generate().stringify, newTask.label, newTask.description, false, newTask.listId, Seq(username))
    collection.flatMap(_.insert.one(insertedTask)).map { _ => insertedTask }
  }

  def updateOne(id: String, newTask: TaskUpdateRequest): Future[Option[Unit]] = {
    val updatedTask = Task(id, newTask.label, newTask.description, newTask.archived, newTask.listId, newTask.membersUsername)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedTask, upsert = false, multi = false))
      .map(verifyUpdatedOneDocument)
  }

  def findOne(id: String): Future[Option[Task]] = {
    collection.flatMap(_.find(idSelector(id)).one[Task])
  }

  def archiveOne(id: String, archiveOrRestore: Boolean): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      idSelector(id),
      BSONDocument("$set" -> BSONDocument("archived" -> archiveOrRestore)),
      fetchNewObject = true)
      .map {
        _.result[Task]
          .map {
            _ =>
          }
      }
    )
  }

  def addMember(id: String, username: String, addedMemberUsername: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          if (isUsernameContainedInTask(addedMemberUsername, task)) Future.successful(Left(BadRequestException("User already has access to this task")))
          else memberRepository.findByUsername(addedMemberUsername).flatMap {
            case Some(_) =>
              addOneMemberToDocument(id, addedMemberUsername)
                .flatMap {
                  _ =>
                    tasksListRepository.addOneMemberToDocument(task.listId, addedMemberUsername)
                      .flatMap {
                        _ =>
                          tasksListRepository.findOne(task.listId)
                            .map {
                              optTaskList => boardRepository.addOneMemberToDocument(optTaskList.get.boardId, addedMemberUsername)
                            }
                      }
                      .map {
                        _ => Right()
                      }
                }
            case None => Future.successful(Left(BadRequestException("User does not exist")))
          }

        case None => Future.successful(Left(NotFoundException("Task not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this task")))
      }
  }

  def deleteMember(id: String, username: String, deletedMemberUsername: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          if (!isUsernameContainedInTask(deletedMemberUsername, task)) Future.successful(Left(BadRequestException("User does not have access to this task")))
          else
            deleteOneMemberFromDocument(id, deletedMemberUsername)
                    .map {
                      _ => Right()
                    }
        case None => Future.successful(Left(NotFoundException("Task not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this task")))
      }
  }

  def archive(id: String, username: String, archiveOrRestore: Boolean): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          archiveOne(id, archiveOrRestore)
            .map {
              _ => Right()
            }
        case None => Future.successful(Left(NotFoundException("Task not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this Task")))
      }
  }

  def create(newTask: TaskCreationRequest, username: String): Future[Either[Exception, Task]] = {
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

  def delete(taskId: String, username: String): Future[Either[Exception, Unit]] = {
    findOne(taskId)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          if (task.archived) {
            deleteOne(taskId)
              .map {
                case Some(_) => Right()
              }
          } else Future.successful(Left(TaskNotArchivedException()))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def find(taskId: String, username: String): Future[Either[Exception, Task]] = {
    findOne(taskId)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) => Future.successful(Right(task))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def update(taskUpdateRequestId: String, taskUpdateRequest: TaskUpdateRequest, username: String): Future[Either[Exception, Unit]] = {
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
              case None => Future.successful(Left(NotFoundException("Task not found")))
              case _ => Future.successful(Left(ForbiddenException("You don't have access to this Task")))
            }
        case None => Future.successful(Left(BadRequestException("List not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this List")))
      }
  }

  def changeList(id: String, username: String, listId: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          tasksListRepository.findOne(listId)
              .flatMap {
                case Some(list) if isUsernameContainedInTasksList(username, list)=>
                updateField(idSelector(id), BSONDocument("listId" -> listId))
                  .map {
                    _ => Right()
                  }
                case None => Future.successful(Left(NotFoundException("List not found")))
                case _ => Future.successful(Left(ForbiddenException("You don't have access to this List")))
              }
        case None => Future.successful(Left(NotFoundException("Task not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this Task")))
      }
  }

  def changeLabel(id: String, username: String, newLabel: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          updateField(idSelector(id), BSONDocument("label" -> newLabel))
            .map {
              _ => Right()
            }
        case None => Future.successful(Left(NotFoundException("Task not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this Task")))
      }
  }

  def changeDescription(id: String, username: String, newDescription: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) if isUsernameContainedInTask(username, task) =>
          updateField(idSelector(id), BSONDocument("description" -> newDescription))
            .map {
              _ => Right()
            }
        case None => Future.successful(Left(NotFoundException("Task not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this Task")))
      }
  }

  private def isUsernameContainedInTask(username: String, task: Task): Boolean = task.membersUsername.contains(username)
}