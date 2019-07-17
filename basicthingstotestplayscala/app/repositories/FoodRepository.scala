package repositories

import javax.inject.Inject
 
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}
 
import scala.concurrent.{ExecutionContext, Future}

sealed trait FoodRepository {

    def listAll: List[Food]

    def listAll(count: Int, offset: Int): List[Food] // cf pagination

    def findOne(id: Int): Future[Option[Food]]

    def updateOne(id: Int): Future[WriteResult]

    def deleteOne(id: Int): Either[NotFound, Unit]

    def createOne(id: Int): Food

}

class MongoFoodRepository @Inject() (reactiveMongoApi: ReactiveMongoApi) extends FoodRepository {

    def collection: Future[BSONCollection] =
        database.map(_.collection[BSONCollection]("food"))

    override def listAll : List[Food] ={
        val cursor: Future[Cursor[BSONDocument]] = collection.map {
        _.find(BSONDocument()).cursor[BSONDocument](ReadPreference.primary)
         }   
        // gather all the BSONDocuments in a list
        val futureFoodsList: Future[List[BSONDocument]] =
        cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[BSONDocument]]()))
        }
    override def listAll(count: Int, offset: Int): List[Food] =
        collection.flatMap(_.find(Json.obj("id" -> id)).one[Food])
         

    override def findOne(id: Int): Option[Food] = 
        collection.flatMap(_.find(Json.obj("id" -> id)).one[Food])

    override def updateOne(id: Int): Either[NotFound, Unit] =
        collection.flatMap(_.update.one(q = BSONDocument("id" -> id), u = food, upsert = false, multi = false))
}