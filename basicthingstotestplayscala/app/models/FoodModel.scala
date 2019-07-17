package models

import javax.inject.Inject
 
import scala.concurrent.{ExecutionContext, Future}

case class Food(id : Int, name : String)
 
object FoodModel {
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
}