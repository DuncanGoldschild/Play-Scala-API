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

//ArrayBuffer qui me sert de "BDD" en attendant la transition vers Mongo ou SQLITE etc
  val foods = ArrayBuffer(
    Food (1, "Carbonara"),
    Food (2, "Bolognaise"),
    Food (3, "Fajitas"),
    Food (4, "Potatoes")
  )

  implicit val foodWrites = Json.writes[Food] //Il faut une implicit pour faire marcher l'écriture d'un JSON
  implicit val foodReads = Json.reads[Food] //Il faut une implicit pour faire marcher la lecture d'un JSON
  /* Les valeurs implicites de READ et WRITE sont utilisées pour la serialisation/déserialisation des JSON
  pour les GET et POST
  Celles implémentées ci-dessus et ci-dessous marchent toutes les 2

  implicit val foodWrites = new Writes[User] {
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
    //Verification que l'objet comporte bien les attributs attendus et que l'ID est bien libre
    if (food.id != null && food.name != null && getById(food.id) == null){
    val newfood = Food(food.id,food.name)
    foods += newfood
    println(Json.toJson(newfood) + "rajouté")
    Ok(Json.toJson(newfood))}
    else Ok("Bad request (existing id or empty name/id)")
  }
  
//Affiche la liste des plats avec GET /foods
  def listFood() = Action{
        Ok(Json.toJson(foods))
  }


//Supprime un plat avec DELETE /delete/food/"id"
  def deleteFood(id : Int) = Action {
    //Verification que l'ID est bien attribué
    if (getById(id) != null){
        val newfood = getById(id)
        foods -= newfood
        println(Json.toJson(newfood) + "supprimé")
        Ok(Json.toJson(newfood))
    }
    else Ok("Plat introuvable")
  }

  //Met un plat existant à jour avec PUT /food/id
  def updateFood(id : Int) =  Action { implicit request =>
    val previousFood = getById(id)
    val food  = Json.fromJson[Food](request.body.asJson.get).get
    //Verification que l'objet comporte bien les attributs attendus, que l'ID est bien existant, et que le nouvel ID est libre
    if (food.id != null && food.name != null && previousFood != null && getById(food.id)==null){
    val newfood = Food(food.id,food.name)
    foods(foods.indexOf(previousFood)) = newfood
    println(Json.toJson(newfood) + "updated")
    Ok(Json.toJson(newfood))}
    else Ok("Bad request (existing id or empty name/id or non existing element)")
  }

//Affiche un plat en particulier avec GET /food/"id"
  def getFood(id: Int) = 
    //Verification que l'ID est bien attribué
      if (getById(id) == null) {
        Action {
          Ok("Incorrect ID")
      }
      }
      else 
        Action {
          Ok(Json.toJson(getById(id)))
        }

//La page de base
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

/**
 *Checks if a food has this ID and returns the Food object
 */
  def getById(id:Int) = {
    val elements = foods.filter(x => (x.id == id))
    if (elements.isEmpty) null else elements(0) 
  }
}

/*
Commande linux pour POST et DELETE

curl     --header "Content-type: application/json"     --request POST     --data '{"id":98, "name":"Salade"}'     http://localhost:9000/foods
curl     --header "Content-type: application/json"     --request DELETE    http://localhost:9000/food/2
curl -X PUT -H "Content-Type: application/json" -d '{"id": 10 , "name" : "spaghetti"}' "http://localhost:9000/food/2"
*/