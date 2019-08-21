package models

import play.api.libs.json.{JsObject, Json, Reads, Writes}
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

case class Board (id: String, label: String, membersUsername: Seq[String])
object Board {

  implicit val boardReader: BSONDocumentReader[Board] = Macros.reader[Board]
  implicit val boardWriter: BSONDocumentWriter[Board] = Macros.writer[Board]

  implicit val boardWrites: Writes[Board] = Json.writes[Board]
  implicit val boardReads: Reads[Board] = Json.reads[Board]

}

case class BoardCreationRequest (label: String)
object BoardCreationRequest{

  implicit val boardCreationRequestReads: Reads[BoardCreationRequest] = Json.reads[BoardCreationRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String"
    )
  }
}

case class BoardUpdateRequest (label: String, membersUsername: Seq[String])
object BoardUpdateRequest{ // todo: delete all full-update ?

  implicit val boardCreationRequestReads: Reads[BoardUpdateRequest] = Json.reads[BoardUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String",
      "membersUsername" -> "Seq[String]"
    )
  }
}

case class TasksList(id: String, label: String, boardId: String, membersUsername: Seq[String])
object TasksList {

  implicit val listReader: BSONDocumentReader[TasksList] = Macros.reader[TasksList]
  implicit val listWriter: BSONDocumentWriter[TasksList] = Macros.writer[TasksList]

  implicit val listWrites: Writes[TasksList] = Json.writes[TasksList]
  implicit val listReads: Reads[TasksList] = Json.reads[TasksList]

}

case class TasksListCreationRequest(label: String, boardId: String)
object TasksListCreationRequest{

  implicit val listCreationRequestReads: Reads[TasksListCreationRequest] = Json.reads[TasksListCreationRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String",
      "boardId" -> "String"
    )
  }
}

case class TasksListUpdateRequest(label: String, membersUsername: Seq[String])
object TasksListUpdateRequest{

  implicit val listUpdateRequestReads: Reads[TasksListUpdateRequest] = Json.reads[TasksListUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String",
      "membersUsername" -> "Seq[String]"
    )
  }

}

case class Task (id: String, label: String, description: String, archived: Boolean, listId: String, membersUsername: Seq[String])
object Task {

  implicit val taskReader: BSONDocumentReader[Task] = Macros.reader[Task]
  implicit val taskWriter: BSONDocumentWriter[Task] = Macros.writer[Task]

  implicit val taskWrites: Writes[Task] = Json.writes[Task]
  implicit val taskReads: Reads[Task] = Json.reads[Task]

}

case class TaskCreationRequest (label: String, description: String, listId: String)
object TaskCreationRequest{

  implicit val taskCreationRequestReads: Reads[TaskCreationRequest] = Json.reads[TaskCreationRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String",
      "description" -> "String",
      "listId" -> "String"
    )
  }
}

case class TaskUpdateRequest (label: String, description: String, archived: Boolean, listId: String, membersUsername: Seq[String])
object TaskUpdateRequest{

  implicit val listUpdateRequestReads: Reads[TaskUpdateRequest] = Json.reads[TaskUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String",
      "description" -> "String",
      "archived" -> "Boolean",
      "listdId" -> "String",
      "membersUsername" -> "Seq[String]"
    )
  }
}

case class ListIdOfTaskUpdateRequest (listId: String)
object ListIdOfTaskUpdateRequest{

  implicit val listIdOfTaskUpdateRequestReads: Reads[ListIdOfTaskUpdateRequest] = Json.reads[ListIdOfTaskUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "listId" -> "String"
    )
  }
}

case class Member (username: String, password: String)
object Member {

  implicit val memberReader: BSONDocumentReader[Member] = Macros.reader[Member]
  implicit val memberWriter: BSONDocumentWriter[Member] = Macros.writer[Member]

  implicit val memberWrites: Writes[Member] = Json.writes[Member]
  implicit val memberReads: Reads[Member] = Json.reads[Member]

  def schema: JsObject = {
    Json.obj(
      "username" -> "String",
      "password" -> "String"
    )
  }
}

case class MemberUpdateRequest (password: String, newPassword: String)
object MemberUpdateRequest {

  implicit val memberUpdateRequestReads: Reads[MemberUpdateRequest] = Json.reads[MemberUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "password" -> "String",
      "newPassword" -> "String"
    )
  }
}

case class MemberAddOrDelete (username: String)
object MemberAddOrDelete {

  implicit val memberAddOrDeleteReads: Reads[MemberAddOrDelete] = Json.reads[MemberAddOrDelete]

  def schema: JsObject = {
    Json.obj(
      "username" -> "String"
    )
  }
}

case class LabelUpdateRequest (label: String)
object LabelUpdateRequest {

  implicit val labelUpdateReads: Reads[LabelUpdateRequest] = Json.reads[LabelUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "label" -> "String"
    )
  }
}

case class ArchiveOrRestoreRequest (archive: Boolean)
object ArchiveOrRestoreRequest {

  implicit val labelUpdateReads: Reads[ArchiveOrRestoreRequest] = Json.reads[ArchiveOrRestoreRequest]

  def schema: JsObject = {
    Json.obj(
      "archive" -> "Boolean"
    )
  }
}

case class DescriptionUpdateRequest (description: String)
object DescriptionUpdateRequest {

  implicit val descriptionUpdateReads: Reads[DescriptionUpdateRequest] = Json.reads[DescriptionUpdateRequest]

  def schema: JsObject = {
    Json.obj(
      "description" -> "String"
    )
  }
}