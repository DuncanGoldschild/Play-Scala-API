package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._

import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID}

import models.{Task, TaskCreationRequest}


class MongoTaskRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GlobalRepository {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("task"))

  def createOne(newTask: TaskCreationRequest): Future[Task] = {
    val insertedTask = Task(BSONObjectID.generate().stringify, newTask.label, newTask.description, newTask.archived, newTask.listId)
    collection.flatMap(_.insert.one(insertedTask)).map { _ => insertedTask }
  }


  def updateOne (id: String, newTask: TaskCreationRequest): Future[Option[Unit]] = {
    val updatedTask = Task(id, newTask.label, newTask.description, newTask.archived, newTask.listId)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedTask, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

}