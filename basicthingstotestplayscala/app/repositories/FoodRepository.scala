package repositories

import models.{Food, FoodWithoutId}
import play.api.mvc._
import javax.inject._

import scala.concurrent._
import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import scala.concurrent.ExecutionContext.Implicits.global


sealed trait FoodRepository {

    def listAll: Future[List[Food]]

    def listAll(count: Int): Future[List[Food]]

    def findOne(id: String): Future[Option[Food]]

    def updateOne(id: String , newFood : FoodWithoutId): Future[Option[Unit]]

    def deleteOne(id: String): Future[Option[Unit]]

    def createOne(newFood : FoodWithoutId): Future[Food]

}

class MongoFoodRepository @Inject() (
    components: ControllerComponents,
    val reactiveMongoApi: ReactiveMongoApi
  ) extends AbstractController(components)
    with MongoController with ReactiveMongoComponents with FoodRepository {

    def collection: Future[BSONCollection] =
        database.map(_.collection[BSONCollection]("food"))

    override def listAll : Future[List[Food]] = {
        val cursor: Future[Cursor[Food]] = collection.map {
        _.find(BSONDocument()).cursor[Food](ReadPreference.primary)
         }   
        // gather all the Foods in a list
        cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[Food]]())
        )
    }
         
    override def listAll(count: Int): Future[List[Food]] = {
      val cursor: Future[Cursor[Food]] = collection.map {
        _.find(BSONDocument()).cursor[Food](ReadPreference.primary)
      }
      // gather all the Foods in a list
      cursor.flatMap(_.collect[List](count, Cursor.FailOnError[List[Food]]()))
    }

    override def findOne(id: String): Future[Option[Food]] = {
        collection.flatMap(_.find(BSONDocument("id" -> id)).one[Food])
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
}