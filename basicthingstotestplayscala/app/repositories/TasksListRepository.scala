package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONObjectID
import models.{TasksList, TasksListCreationRequest, TasksListUpdateRequest}


class MongoListTaskRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository [TasksList] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("listTask"))

  def createOne(newListTask: TasksListCreationRequest, username : String): Future[TasksList] = {
    val insertedListTask = TasksList(BSONObjectID.generate().stringify, newListTask.label, newListTask.boardId, Seq(username))
    collection.flatMap(_.insert.one(insertedListTask)).map { _ => insertedListTask }
  }


  def updateOne (id: String, newListTask: TasksListUpdateRequest): Future[Option[Unit]] = {
    val updatedListTask = TasksList(id, newListTask.label, newListTask.boardId, newListTask.membersUsername)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedListTask, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

}