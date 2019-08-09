package models

import play.api.libs.json.{Json, Reads, Writes}

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

}

case class BoardUpdateRequest (label: String, membersUsername: Seq[String])
object BoardUpdateRequest{

  implicit val boardCreationRequestReads: Reads[BoardUpdateRequest] = Json.reads[BoardUpdateRequest]

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

}

case class TasksListUpdateRequest(label: String, membersUsername: Seq[String])
object TasksListUpdateRequest{

  implicit val listUpdateRequestReads: Reads[TasksListUpdateRequest] = Json.reads[TasksListUpdateRequest]

}

case class Task (id: String, label: String, description: String, archived: Boolean, listId: String, membersUsername: Seq[String])
object Task {

  implicit val taskReader: BSONDocumentReader[Task] = Macros.reader[Task]
  implicit val taskWriter: BSONDocumentWriter[Task] = Macros.writer[Task]

  implicit val taskWrites: Writes[Task] = Json.writes[Task]
  implicit val taskReads: Reads[Task] = Json.reads[Task]

}

case class TaskCreationRequest (label: String, description: String, archived: Boolean, listId: String)
object TaskCreationRequest{

  implicit val taskCreationRequestReads: Reads[TaskCreationRequest] = Json.reads[TaskCreationRequest]

}

case class TaskUpdateRequest (label: String, description: String, archived: Boolean, listId: String, membersUsername: Seq[String])
object TaskUpdateRequest{

  implicit val listUpdateRequestReads: Reads[TaskUpdateRequest] = Json.reads[TaskUpdateRequest]

}

case class ListIdOfTaskUpdateRequest (listId: String)
object ListIdOfTaskUpdateRequest{

  implicit val listIdOfTaskUpdateRequestReads: Reads[ListIdOfTaskUpdateRequest] = Json.reads[ListIdOfTaskUpdateRequest]

}

case class Member (username: String, password: String)
object Member {

  implicit val memberReader: BSONDocumentReader[Member] = Macros.reader[Member]
  implicit val memberWriter: BSONDocumentWriter[Member] = Macros.writer[Member]

  implicit val memberWrites: Writes[Member] = Json.writes[Member]
  implicit val memberReads: Reads[Member] = Json.reads[Member]

}

case class MemberUpdateRequest (password: String, newPassword: String)
object MemberUpdateRequest {

  implicit val memberUpdateRequestReads: Reads[MemberUpdateRequest] = Json.reads[MemberUpdateRequest]

}

case class MemberAddOrDelete (username: String)
object MemberAddOrDelete {

  implicit val memberAddOrDeleteReads: Reads[MemberAddOrDelete] = Json.reads[MemberAddOrDelete]

}

case class LabelUpdateRequest (label: String)
object LabelUpdateRequest {

  implicit val labelUpdateReads: Reads[LabelUpdateRequest] = Json.reads[LabelUpdateRequest]

}

case class DescriptionUpdateRequest (description: String)
object DescriptionUpdateRequest {

  implicit val descriptionUpdateReads: Reads[DescriptionUpdateRequest] = Json.reads[DescriptionUpdateRequest]

}