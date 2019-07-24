package repositories

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}


trait GlobalRepository {

  def collection : Future[BSONCollection]

  def listAll[A : BSONDocumentReader]: Future[List[A]] = {
      listAll(-1)
    }

  def listAll[A : BSONDocumentReader](count: Int): Future[List[A]] = {
      val cursor: Future[Cursor[A]] = collection.map {
      _.find(BSONDocument()).cursor[A](ReadPreference.primary)
    }
      // gather all the Boards in a list
      cursor.flatMap(_.collect[List](count, Cursor.FailOnError[List[A]]()))
  }

  def listAllFromIds [A : BSONDocumentReader](boardIds: Seq[String]): Future[List[A]] = {
    val cursor: Future[Cursor[A]] = collection.map {
      _.find(BSONDocument("id" -> BSONDocument("$in" -> boardIds) )).cursor[A](ReadPreference.primary)
    }
    // gather all the Boards in a list
    cursor.flatMap(_.collect[List](-1, Cursor.FailOnError[List[A]]()))
  }

  def findOne[A : BSONDocumentReader](id: String): Future[Option[A]] = {
    collection.flatMap(_.find(idSelector(id)).one[A])
  }

  def deleteOne(id: String): Future[Option[Unit]] = {
    collection.flatMap(_.delete.one(idSelector(id)))
      .map(verifyUpdatedOneDocument)
  }


  def idSelector (id: String) = BSONDocument("id" -> id)

  def verifyUpdatedOneDocument(writeResult: WriteResult): Option[Unit] =
    if (writeResult.n == 1 && writeResult.ok) Some() else None
}
