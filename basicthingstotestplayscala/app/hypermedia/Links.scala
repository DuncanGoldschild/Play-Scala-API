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

  def addAsJsonTo(jsValue: JsValue, elementsName: String): JsObject = {
    jsValue.as[JsObject] ++ Json.obj(elementsName -> asJson)
  }

  def asJson: JsValue = {
    Json.toJson(controls)
  }

  def addAsJsonTo[T](obj: T,  elementsName: String)(implicit objWrites: Writes[T]): JsObject = {
    Json.toJson(obj).as[JsObject] ++ Json.obj(elementsName -> asJson)
  }

}

