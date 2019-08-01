/*package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._

import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{WriteResult}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}

import models.{Food, FoodWithoutId}


sealed trait FoodRepository {

    def listAll: Future[List[Food]]

    def listAll(count: Int): Future[List[Food]]

    def findOne(id: String): Future[Option[Food]]

    def updateOne(id: String , newFood: FoodWithoutId): Future[Option[Unit]]

    def deleteOne(id: String): Future[Option[Unit]]

    def createOne(newFood: FoodWithoutId): Future[Food]

}

class MongoFoodRepository @Inject() (
    components: ControllerComponents,
    val reactiveMongoApi: ReactiveMongoApi
  ) extends AbstractController(components)
      with MongoController
      with ReactiveMongoComponents
      with FoodRepository {

    def collection: Future[BSONCollection] =
        database.map(_.collection[BSONCollection]("food"))

    override def listAll: Future[List[Food]] = {
      listAll(-1)
    }
         
    override def listAll(count: Int): Future[List[Food]] = {
      val cursor: Future[Cursor[Food]] = collection.map {
        _.find(BSONDocument()).cursor[Food](ReadPreference.primary)
      }
      // gather all the Foods in a list
      cursor.flatMap(_.collect[List](count, Cursor.FailOnError[List[Food]]()))
    }

    override def findOne(id: String): Future[Option[Food]] = {
        collection.flatMap(_.find(idSelector(id)).one[Food])
    }

    override def createOne(newFood: FoodWithoutId): Future[Food] = {
      val insertedFood = Food(BSONObjectID.generate().stringify, newFood.name)
      collection.flatMap(_.insert.one(insertedFood)).map { _ => insertedFood }
    }

    override def deleteOne(id: String): Future[Option[Unit]] = {
      collection.flatMap(_.delete.one(idSelector(id)))
        .map(verifyUpdatedOneDocument)
    }

    override def updateOne(id: String, newFood: FoodWithoutId): Future[Option[Unit]] = {
      val updatedFood = Food(id, newFood.name)
      collection.flatMap(_.update.one(q = idSelector(id), u = updatedFood, upsert = false, multi = false))
        .map (verifyUpdatedOneDocument)
    }

  def idSelector (id: String) = BSONDocument("id" -> id)

  def verifyUpdatedOneDocument(writeResult: WriteResult): Option[Unit] =
    if (writeResult.n == 1 && writeResult.ok) Some() else None
}*/