package controllers


import scala.collection.mutable._
import javax.inject._
import scala.util.{ Failure, Success }
import scala.concurrent._
import ExecutionContext.Implicits.global

import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.play.json._, collection._

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
  MongoController,
  ReactiveMongoApi,
  ReactiveMongoComponents
}

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import com.google.inject.Singleton

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class FoodController @Inject() (
  components: ControllerComponents,
  val reactiveMongoApi: ReactiveMongoApi
) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents {

case class Food(id : Int, name : String)

  def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("Food"))
    

implicit object FoodWriter extends BSONDocumentWriter[Food] {
  def write(food: Food): BSONDocument =
    BSONDocument("id" -> food.id, "name" -> food.name)
}

implicit object FoodReader extends BSONDocumentReader[Food] {
  def read(bson: BSONDocument): Food = {
    val opt: Option[Food] = for {
      name <- bson.getAs[String]("name")
      id <- bson.getAs[BSONNumberLike]("id").map(_.toInt)
    } yield new Food(id, name)

    opt.get // the person is required (or let throw an exception)
  }
}

  implicit val foodWrites = Json.writes[Food] 
  implicit val foodReads = Json.reads[Food] 

// Display the Food by its id with GET /food/"id"
  def findById(id: Int) = Action.async {
  collection.flatMap(_.find(Json.obj("id" -> id)).one[Food]).map{ 
    case Some(p) => Ok(Json.toJson(p))
    case None    => Ok("Person Not Found.")
  }.recover{ case t: Throwable =>
    Ok("error")
  } 
}

  // Display all Food elements with GET /foods
  def listFood = Action.async {
    val cursor: Future[Cursor[BSONDocument]] = collection.map {
      _.find(BSONDocument()).
        // perform the query and get a cursor of BSONDocument
        cursor[BSONDocument](ReadPreference.primary)
    }
  
    // gather all the BSONDocuments in a list
    val futureFoodsList: Future[List[BSONDocument]] =
      cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[BSONDocument]]()))

    futureFoodsList.map { food =>
      Ok(Json.toJson(food))
    }
  
  }

  // Delete with DELETE /food/"id"
  def deleteFood(id : Int) =  Action {
    val selector1 = BSONDocument("id" -> id)

    val futureRemove1 = collection.map {_.delete.one(selector1)}

    futureRemove1.onComplete { // callback
      case Failure(e) => throw e
      case Success(writeResult) => Created
    }
    // TODO : request handler cannot be type UNIT
    Ok("")

  }

  // Add with POST /foods
  def newFood = Action.async(parse.json) { request =>
      request.body.validate[Food].map { food =>
        collection.flatMap(_.insert.one(food)).map { lastError =>
          Logger.debug(s"Successfully inserted with LastError: $lastError")
          Created
        }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  } 

  // Update with PUT /food/"id"
  def updateFood(id : Int) = Action.async(parse.json) { request =>
    request.body.validate[Food].map { food =>
      collection.flatMap(_.update.one(q = BSONDocument("id" -> id), u = food, upsert = false, multi = false)).map { lastError =>
        Logger.debug(s"Successfully updated with LastError: $lastError")
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  // Home
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }  
}