package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._

import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID}

import models.{ListTask, ListTaskCreationRequest}


class MongoListTaskRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GlobalRepository {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("listTask"))

  def createOne(newListTask: ListTaskCreationRequest): Future[ListTask] = {
    val insertedListTask = ListTask(BSONObjectID.generate().stringify, newListTask.label, newListTask.boardId)
    collection.flatMap(_.insert.one(insertedListTask)).map { _ => insertedListTask }
  }


  def updateOne (id: String, newListTask: ListTaskCreationRequest): Future[Option[Unit]] = {
    val updatedListTask = ListTask(id, newListTask.label, newListTask.boardId)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedListTask, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

}