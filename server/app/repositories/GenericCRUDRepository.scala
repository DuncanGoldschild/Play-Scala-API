package repositories

import models.{Board, Task, TasksList}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}


trait GenericCRUDRepository [A] {
    def collection: Future[BSONCollection]

  def listAll(implicit bsonReader: BSONDocumentReader[A]): Future[List[A]] = listAll(-1)

  def listAll(count: Int)(implicit bsonReader: BSONDocumentReader[A]): Future[List[A]] = {
      val cursor: Future[Cursor[A]] = collection.map {
      _.find(BSONDocument()).cursor[A](ReadPreference.primary)
    }
      // gather all the Boards in a list
      cursor.flatMap(_.collect[List](count, Cursor.FailOnError[List[A]]()))
  }

  def listAllFromUsername(username: String)(implicit bsonReader: BSONDocumentReader[A]): Future[List[A]] = {
    val cursor: Future[Cursor[A]] = collection.map {
      _.find(BSONDocument("membersUsername" -> BSONDocument("$eq" -> username) )).cursor[A](ReadPreference.primary)
    }
    // gather all the Boards in a list
    cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[A]]()))
  }

  def findOne(id: String)(implicit bsonReader: BSONDocumentReader[A]): Future[Option[A]] = {
    collection.flatMap(_.find(idSelector(id)).one[A])
  }

  def deleteOne(id: String): Future[Option[Unit]] = {
    collection.flatMap(_.delete.one(idSelector(id)))
      .map(verifyUpdatedOneDocument)
  }

  def idSelector (id: String) = BSONDocument("id" -> id)

  def verifyUpdatedOneDocument(writeResult: WriteResult): Option[Unit] =
    if (writeResult.n == 1 && writeResult.ok) Some() else None

  def isUsernameContainedInBoard (username: String, board: Board): Boolean = board.membersUsername.contains(username)

  def isUsernameContainedInTasksList (username: String, tasksList: TasksList): Boolean = tasksList.membersUsername.contains(username)

  def listAllListsFromBoardId(id: String): Future[List[TasksList]] = {
    val cursor: Future[Cursor[TasksList]] = collection.map {
      _.find(BSONDocument("boardId" -> id)).cursor[TasksList](ReadPreference.primary)
    }
    cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[TasksList]]()))
  }

  def listAllTasksFromListId(id: String): Future[List[Task]] = {
    val cursor: Future[Cursor[Task]] = collection.map {
      _.find(BSONDocument("listId" -> id)).cursor[Task](ReadPreference.primary)
    }
    cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[Task]]()))
  }

  def addOneMemberToDocument(id: String, addedUsername: String)(implicit bsonReader: BSONDocumentReader[A]): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      idSelector(id),
      BSONDocument("$addToSet" -> BSONDocument("membersUsername" -> addedUsername)),
      fetchNewObject = true)
      .map {
        _.result[A]
          .map {
            _ =>
          }
      }
    )
  }

  def deleteOneMemberFromDocument(id: String, deletedUsername: String)(implicit bsonReader: BSONDocumentReader[A]): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      idSelector(id),
      BSONDocument("$pull" -> BSONDocument("membersUsername" -> deletedUsername)),
      fetchNewObject = true)
      .map {
        _.result[A]
          .map {
            _ =>
          }
      }
    )
  }

  def deleteOneMemberFromAllDocumentSelected(selector: BSONDocument, deletedUsername: String)(implicit bsonReader: BSONDocumentReader[A]): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      selector,
      BSONDocument("$pull" -> BSONDocument("membersUsername" -> deletedUsername)),
      fetchNewObject = true)
      .map {
        _.result[A]
          .map {
            _ =>
          }
      }
    )
  }

  def deleteAllDocumentSelected(selector: BSONDocument)(implicit bsonReader: BSONDocumentReader[A]): Future[Option[Unit]] = {
    collection.flatMap(_.delete.one(selector))
      .map(verifyUpdatedOneDocument)
  }

  def updateField(selector: BSONDocument, field: BSONDocument): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      selector,
      BSONDocument("$set" -> field),
      fetchNewObject = true)
      .map {
        _.result[Task]
          .map {
            _ =>
          }
      }
    )
  }
}