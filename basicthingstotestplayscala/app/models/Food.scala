package models

import reactivemongo.bson._
import play.api.libs.json._

case class FoodWithoutId(name : String)
object FoodWithoutId {

  implicit object FoodWithoutIdReader extends BSONDocumentReader[FoodWithoutId] {
    def read(bson: BSONDocument): FoodWithoutId = {
      val opt: Option[FoodWithoutId] = for {
        name <- bson.getAs[String]("name")
      } yield new FoodWithoutId(name)
      opt.get // the Food is required (or let throw an exception)
    }
  }

  implicit val foodWithoutIdReads: Reads[FoodWithoutId] = Json.reads[FoodWithoutId]
}

case class Food(id : String, name: String)
object Food {

  implicit object FoodWriter extends BSONDocumentWriter[Food] {
    def write(food: Food): BSONDocument =
      BSONDocument("id" -> food.id, "name" -> food.name)
  }

  implicit object FoodReader extends BSONDocumentReader[Food] {
    def read(bson: BSONDocument): Food = {
      val opt: Option[Food] = for {
        name <- bson.getAs[String]("name")
        id <- bson.getAs[String]("id")
      } yield new Food(id, name)
      opt.get // the Food is required (or let throw an exception)
    }
  }

  implicit val foodWrites: Writes[Food] = Json.writes[Food]
  implicit val foodReads: Reads[Food] = Json.reads[Food]

}