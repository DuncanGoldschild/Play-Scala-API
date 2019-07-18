package controllers


import scala.concurrent._
import scala.collection.Seq

import javax.inject._

import ExecutionContext.Implicits.global

import reactivemongo.play.json._

import play.api.mvc._
import play.api.libs.json._
import play.api.Logger

import com.google.inject.Singleton

import repositories.MongoFoodRepository

import models.FoodWithoutId


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class FoodController @Inject() (
  components: ControllerComponents,
  foodRepository: MongoFoodRepository
) extends AbstractController(components) {

  private val logger = Logger(this.getClass)

// Display the Food by its id with GET /food/"id"
  def findById(id: String): Action[AnyContent] = Action.async {
  foodRepository.findOne(id)
    .map{
      case Some(food) => Ok(Json.toJson(food))
      case None => NotFound
    }.recover(logAndInternalServerError)
  }

  // Display all Food elements with GET /foods
  def listFood: Action[AnyContent] = Action.async {
    foodRepository.listAll.map{
      list => Ok(Json.toJson(list))
    }.recover(logAndInternalServerError)
  }

  // Delete with DELETE /food/"id"
  def deleteFood(id : String): Action[AnyContent] =  Action.async {
    foodRepository.deleteOne(id)
    .map{
      case Some(_) => NoContent
      case None => NotFound
    }.recover(logAndInternalServerError)
  }

  // Add with POST /foods
  def newFood: Action[JsValue] = Action.async(parse.json) { request =>
      val foodResult = request.body.validate[FoodWithoutId]
      foodResult.fold(
        errors => {
          badRequest(errors)
        },
        food => {
            foodRepository.createOne(food).map{
              createdFood => Ok(Json.toJson(createdFood))
            }.recover(logAndInternalServerError)
        }
      )
  } 

  // Update with PUT /food/"id"
  def updateFood(id : String): Action[JsValue] = Action.async(parse.json) { request =>
    val foodResult = request.body.validate[FoodWithoutId]
    foodResult.fold(
      errors => {
        badRequest(errors)
      },
      food => {
          foodRepository.updateOne(id,food)
            .map {
              case Some(_) => NoContent
              case None => NotFound
            }.recover(logAndInternalServerError)
      }
    )
  }

  private def badRequest (errors : Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] = {
    Future.successful(BadRequest(Json.obj("status" -> "Error", "message" -> JsError.toJson(errors))))
  }

  private def logAndInternalServerError: PartialFunction[Throwable, Result] = {
    case e : Throwable =>
      logger.error(e.getMessage, e)
      InternalServerError

  }
}