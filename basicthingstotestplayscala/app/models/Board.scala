package models

import play.api.libs.json.{Json, Reads, Writes}

import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

case class Board (id: String, label: String)
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

case class ListTask (id: String, label: String, boardId: String)
object ListTask {

  implicit val listReader: BSONDocumentReader[ListTask] = Macros.reader[ListTask]
  implicit val listWriter: BSONDocumentWriter[ListTask] = Macros.writer[ListTask]

  implicit val listWrites: Writes[ListTask] = Json.writes[ListTask]
  implicit val listReads: Reads[ListTask] = Json.reads[ListTask]

}

case class ListTaskCreationRequest (label: String, boardId: String)
object ListTaskCreationRequest{

  implicit val listCreationRequestReads: Reads[ListTaskCreationRequest] = Json.reads[ListTaskCreationRequest]

}

case class Task (id: String, label: String, description: String, archived : Boolean, listId: String)
object Task {

  implicit val taskReader: BSONDocumentReader[Task] = Macros.reader[Task]
  implicit val taskWriter: BSONDocumentWriter[Task] = Macros.writer[Task]

  implicit val taskWrites: Writes[Task] = Json.writes[Task]
  implicit val taskReads: Reads[Task] = Json.reads[Task]

}

case class TaskCreationRequest (label: String, description: String, archived : Boolean, listId: String)
object TaskCreationRequest{

  implicit val taskCreationRequestReads: Reads[TaskCreationRequest] = Json.reads[TaskCreationRequest]

}

case class Member (username: String, password: String, boardsId: Seq[String], tasksId: Seq[String])
object Member {

  implicit val memberReader: BSONDocumentReader[Member] = Macros.reader[Member]
  implicit val memberWriter: BSONDocumentWriter[Member] = Macros.writer[Member]

  implicit val memberWrites: Writes[Member] = Json.writes[Member]
  implicit val memberReads: Reads[Member] = Json.reads[Member]

}

case class MemberCreationRequest (username: String, password: String, boardsId: Seq[String], tasksId: Seq[String]) // Members creation
object MemberCreationRequest{

  implicit val MemberCreationRequestReads: Reads[MemberCreationRequest] = Json.reads[MemberCreationRequest]
  implicit val memberWriter: BSONDocumentWriter[MemberCreationRequest] = Macros.writer[MemberCreationRequest]
}