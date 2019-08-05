package hypermedia

import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes


case class Links() {

  var controls: List[BoardLink] = List()

  def add(link: BoardLink) {
    controls = link :: controls
  }

  def addAsJsonTo(jsValue: JsValue): JsObject = {
    jsValue.as[JsObject] ++ asJson
  }

  def asJson: JsObject = {
    Json.obj(
      "@controls" -> Json.toJson(controls))
  }

  def addAsJsonTo[T](obj: T)(implicit objWrites: Writes[T]): JsObject = {
    Json.toJson(obj).as[JsObject] ++ asJson
  }

}

object Links {

  def generateAsJson[T](obj: T, linkGenerator: T => Links)(implicit objWrites: Writes[T]): JsObject = {
    Json.toJson(obj).as[JsObject] ++ linkGenerator(obj).asJson
  }

  def generateAsJson[T](list: Seq[T], linkGenerator: T => Links)(implicit objWrites: Writes[T]): JsObject = {
    JsObject(Seq("collection" -> Json.toJson(list.map(generateAsJson(_, linkGenerator)))))
  }

  def generateAsJsonImplicit[T](obj: T)(implicit linkGenerator: T => Links, objWrites: Writes[T]): JsObject = {
    Json.toJson(obj).as[JsObject] ++ linkGenerator(obj).asJson
  }

  def generateAsJsonImplicit[T](list: Seq[T])(implicit linkGenerator: T => Links, objWrites: Writes[T]): JsObject = {
    JsObject(Seq("collection" -> Json.toJson(list.map(generateAsJson(_, linkGenerator)))))
  }
}

