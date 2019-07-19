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

import repositories.MongoBoardRepository

import models.BoardCreationRequest


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class BoardController @Inject() (
                                 components: ControllerComponents,
                                 boardRepository: MongoBoardRepository
                               ) extends AbstractController(components) {

  private val logger = Logger(this.getClass)

  // Display the board by its id with GET /board/"id"
  def findBoardById(id: String): Action[AnyContent] = Action.async {
    boardRepository.findOne(id)
      .map{
        case Some(board) => Ok(Json.toJson(board))
        case None => NotFound
      }.recover(logAndInternalServerError)
  }

  // Display all board elements with GET /boards
  def allBoards: Action[AnyContent] = Action.async {
    boardRepository.listAll.map{
      list => Ok(Json.toJson(list))
    }.recover(logAndInternalServerError)
  }

  // Delete with DELETE /board/"id"
  def deleteBoard(id : String): Action[AnyContent] =  Action.async {
    boardRepository.deleteOne(id)
      .map{
        case Some(_) => NoContent
        case None => NotFound
      }.recover(logAndInternalServerError)
  }

  // Add with POST /boards
  def createNewBoard: Action[JsValue] = Action.async(parse.json) { request =>
    val boardResult = request.body.validate[BoardCreationRequest]
    boardResult.fold(
      errors => {
        badRequest(errors)
      },
      board => {
        boardRepository.createOne(board).map{
          createdBoard => Ok(Json.toJson(createdBoard))
        }.recover(logAndInternalServerError)
      }
    )
  }

  // Update with PUT /board/"id"
  def updateBoard(id : String): Action[JsValue] = Action.async(parse.json) { request =>
    val boardResult = request.body.validate[BoardCreationRequest]
    boardResult.fold(
      errors => {
        badRequest(errors)
      },
      board => {
        boardRepository.updateOne(id,board)
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