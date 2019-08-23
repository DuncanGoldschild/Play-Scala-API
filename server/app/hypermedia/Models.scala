package hypermedia

import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.Call

case class HypermediaControl(
                              title: String,
                              href: String,
                              verb: String,
                              mediaType: String,
                              schema: Option[JsObject]
                            ) {
  def toJson: JsValue = Json.toJson(this)
}

object HypermediaControl {

  type EmbeddedEntity = Resource[Reference]

  def apply(title: String, call: Call, mediaType: String, schema: JsObject): HypermediaControl =
    HypermediaControl(title, call.url, call.method, mediaType, Some(schema))

  def apply(title: String, call: Call, mediaType: String): HypermediaControl =
    HypermediaControl(title, call.url, call.method, mediaType, None)

  def reference(id: String, label: String, selfControl: HypermediaControl): EmbeddedEntity
    = new Resource(Reference(id, label), Map("self" -> selfControl))

  implicit lazy val writes: Writes[HypermediaControl] = Json.writes[HypermediaControl]()

}

case class Reference(id: String, label: String)

object Reference {
  implicit val writes: Writes[Reference] = Json.writes[Reference]
}