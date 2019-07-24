package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONObjectID
import models.{Task, TaskCreationRequest, TaskUpdateRequest}



class MongoTaskRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GlobalRepository {

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
  override def deleteOne(id: String): Future[Option[Unit]] = {
    findOne(id)
      .flatMap {
        case Some(task) => if (task.archived) {
          collection.flatMap(_.delete.one(idSelector(id)))
            .map(verifyUpdatedOneDocument)
        } else Future.successful(None)
        case None => Future.successful(None)
    }
  }
}