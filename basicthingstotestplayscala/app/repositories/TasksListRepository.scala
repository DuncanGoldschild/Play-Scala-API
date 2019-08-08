package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.{BadRequestException, ForbiddenException, NotFoundException, TasksList, TasksListCreationRequest, TasksListUpdateRequest}
import reactivemongo.api.{Cursor, ReadPreference}


class MongoTasksListRepository @Inject()(
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi,
                                       boardRepository: MongoBoardRepository,
                                       memberRepository: MongoMemberRepository
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository [TasksList] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("listTask"))

  def createOne(newListTask: TasksListCreationRequest, username: String): Future[TasksList] = {
    val insertedListTask = TasksList(BSONObjectID.generate().stringify, newListTask.label, newListTask.boardId, Seq(username))
    collection.flatMap(_.insert.one(insertedListTask)).map { _ => insertedListTask }
  }


  def updateOne (id: String, newListTask: TasksListUpdateRequest, boardId: String): Future[Option[Unit]] = {
    val updatedListTask = TasksList(id, newListTask.label, boardId, newListTask.membersUsername)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedListTask, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

  def addMember(id: String, username: String, addedMemberUsername: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) =>
          if (isUsernameContainedInTasksList(addedMemberUsername, tasksList)) Future.successful(Left(BadRequestException("User already has access to this tasksList")))
          else memberRepository.findByUsername(addedMemberUsername).flatMap {
            case Some(_) =>
              addOneMemberToDocument(id, addedMemberUsername)
                .map {
                  _ => Right()
                }
            case None => Future.successful(Left(BadRequestException("User does not exist")))
          }

        case None => Future.successful(Left(NotFoundException("List not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this tasksList")))
      }
  }

  def deleteMember(id: String, username: String, deletedMemberUsername: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) =>
          if (!isUsernameContainedInTasksList(deletedMemberUsername, tasksList)) Future.successful(Left(BadRequestException("User does not have access to this tasksList")))
          else
            deleteOneMemberFromDocument(id, deletedMemberUsername)
              .map {
                _ => Right()
              }
        case None => Future.successful(Left(NotFoundException("List not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this tasksList")))
      }
  }
  
  def create(newListTask: TasksListCreationRequest, username: String): Future[Either[Exception, TasksList]] = {
    boardRepository.findOne(newListTask.boardId)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) =>
          createOne(newListTask, username)
            .map {
              createdListTask => Right(createdListTask)
            }
        case None => Future.successful(Left(BadRequestException("Board does not exist")))
        case _ => Future.successful(Left(ForbiddenException("You dont have access to this board")))
      }
  }

  def update (tasksListUpdateRequestId: String, tasksListUpdateRequest: TasksListUpdateRequest, username: String): Future[Either[Exception, Unit]] = {
    findOne(tasksListUpdateRequestId)
      .flatMap {
        case Some(tasksList: TasksList) if isUsernameContainedInTasksList(username, tasksList) =>
          updateOne(tasksListUpdateRequestId, tasksListUpdateRequest, tasksList.boardId)
            .map {
              case Some (_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def delete (tasksListId: String, username: String): Future[Either[Exception, Unit]] = {
    findOne(tasksListId)
      .flatMap {
        case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) =>
          deleteOne(tasksListId)
            .map {
              case Some (_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def find (tasksListId: String, username: String): Future[Either[Exception, TasksList]] = {
    findOne(tasksListId)
      .flatMap {
        case Some(tasksList) if isUsernameContainedInTasksList(username, tasksList) => Future.successful(Right(tasksList))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }
}