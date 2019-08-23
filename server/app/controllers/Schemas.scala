package controllers

import javax.inject.Inject
import models._
import play.api.libs.json.JsObject
import play.api.mvc._

class Schemas @Inject() (components: ControllerComponents) extends AbstractController(components) {

  def findSchemaByName(name: String): Action[AnyContent] = Action {
    Schemas.schemas.get(name).map(Ok(_)).getOrElse(NotFound)
  }

}

object Schemas {

  lazy val schemas: Map[String, JsObject] = Map(
    "createBoard" -> createBoardSchema,
    "updateBoard" -> updateBoardSchema,
    "addDeleteMember" -> addDeleteMemberSchema,
    "updateLabel" -> updateLabelSchema,
    "updateDescription" -> updateDescriptionSchema,
    "createList" -> createListSchema,
    "createTask" -> createTaskSchema,
    "changeList" -> changeListSchema,
    "updatePassword" -> updatePasswordSchema,
    "auth" -> authSchema,
    "archiveOrRestore" -> archiveOrRestoreSchema,
    "updateTask" -> updateTaskSchema
  )

  //Todo : all other schemas and routes
  val createBoardSchema: JsObject = BoardCreationRequest.schema
  val updateBoardSchema: JsObject = ArchiveOrRestoreRequest.schema
  val addDeleteMemberSchema: JsObject = TaskUpdateRequest.schema
  val updateLabelSchema: JsObject = Member.schema
  val updateDescriptionSchema: JsObject = MemberUpdateRequest.schema
  val createListSchema: JsObject = TasksListCreationRequest.schema
  val createTaskSchema: JsObject = TaskCreationRequest.schema
  val changeListSchema: JsObject = BoardUpdateRequest.schema
  val updatePasswordSchema: JsObject = MemberAddOrDelete.schema
  val authSchema: JsObject = LabelUpdateRequest.schema
  val archiveOrRestoreSchema: JsObject = DescriptionUpdateRequest.schema
  val updateTaskSchema: JsObject = ListIdOfTaskUpdateRequest.schema

}