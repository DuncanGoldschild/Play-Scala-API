package models

import reactivemongo.bson._

import play.api.libs.json._

case class FoodWithoutId(name : String)

object FoodWithoutId {

  implicit val foodWithoutIdReads: Reads[FoodWithoutId] = Json.reads[FoodWithoutId]

}

case class Food(id : String, name: String)

object Food {

  implicit val foodReader: BSONDocumentReader[Food] = Macros.reader[Food]
  implicit val foodWriter: BSONDocumentWriter[Food] = Macros.writer[Food]

  implicit val foodWrites: Writes[Food] = Json.writes[Food]
  implicit val foodReads: Reads[Food] = Json.reads[Food]

}