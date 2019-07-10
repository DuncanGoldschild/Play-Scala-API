package controllers


import scala.collection.mutable._
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import com.google.inject.Singleton
import play.api.libs.functional.syntax._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

case class Food(id : Int, name : String)

  val foods = ArrayBuffer(
    Food (1, "Carbonara"),
    Food (2, "Bolognaise"),
    Food (3, "Fajitas"),
    Food (4, "Potatoes")
  )

  implicit val foodWrites = Json.writes[Food] //pas trop compris comment ce truc marche
  implicit val foodReads = Json.reads[Food] //pas trop compris comment ce truc marche
  /* Les valeurs implicites de READ et WRITE sont utilisées pour la serialisation/déserialisation des JSON
  pour les GET et POST
  Celles implémentées ci-dessus et ci-dessous marchent toutes les 2

  implicit val userWrites = new Writes[User] {
    def writes(user: User) = Json.obj(
      "id" -> user.id,
      "email" -> user.email,
      "firstName" -> user.firstName,
      "lastName" -> user.lastName
    )
  }*/

  /*implicit val foodReads: Reads[Food] = (
    (__ \ "id").read[Int] and
      (__ \ "name").read[String]
    )(Food.apply _)
*/



//Ajoute un plat avec POST /foods
  def newFood = Action { implicit request =>
    val food  = Json.fromJson[Food](request.body.asJson.get).get
    if (food.id != null && food.name != null && getById(food.id) == null){
    val newfood = Food(food.id,food.name)
    foods += newfood
    println(Json.toJson(newfood) + "rajouté")
    Ok(Json.toJson(newfood))}
    else Ok("Bad request (existing food or empty name/id)")
  }
  
//Affiche la liste des plats avec GET /foods
  def listFood() = Action{
        Ok(Json.toJson(foods))
  }

//Supprime un plat
  def deleteFood(id : Int) = Action {
    if (getById(id) != null){
        val newfood = getById(id)
        foods -= newfood
        println(Json.toJson(newfood) + "supprimé")
        Ok(Json.toJson(newfood))
    }
    else Ok("Plat introuvable")
  }

//Affiche un plat en particulier avec GET /food/"id"
  def getFood(id: Int) = 
      if (getById(id) == null) {
        Action {
          Ok("Incorrect ID")
      }
      }
      else 
        Action {
          Ok(Json.toJson(getById(id)))
        }

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def getById(id:Int) = {
    val elements = foods.filter(x => (x.id == id))
    if (elements.isEmpty) null else elements(0) 
  }
}

/*
Commande linux pour POST et DELETE

curl     --header "Content-type: application/json"     --request POST     --data '{"id":98, "name":"Salade"}'     http://localhost:9000/foods
curl     --header "Content-type: application/json"     --request DELETE    http://localhost:9000/delete/food/98

*/