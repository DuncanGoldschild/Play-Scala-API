package hypermedia

import hypermedia.HypermediaControl.EmbeddedEntity
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.Call

object Hypermedia {

  def writeResponseDocument[A](info: A, embeddedEntities: List[EmbeddedEntity], selfControls: Map[String, HypermediaControl])(implicit writesA: Writes[A]) = {
    val resource = new Resource(info, embeddedEntities, selfControls)
    Json.toJson(resource)(resource.writes)
  }

  def writeResponseDocument[A](info: A, selfControls: Map[String, HypermediaControl])(implicit writesA: Writes[A]) = {
    val resource = new Resource(info, selfControls)
    Json.toJson(resource)(resource.writes)
  }

  def createControl(id: String, label: String, name: String, uri: String, verb: String, mediaType: String) =
    HypermediaControl.reference(id, label, HypermediaControl("Get: "+label, uri, verb, mediaType, None))

  def createControl(id: String, label: String, name: String, call: Call, mediaType: String) =
    createControl(id, label, name, call.url, call.method, mediaType)

  def createControl(name: String, title: String, uri: String, verb: String, mediaType: String) =
    name -> HypermediaControl(title, uri, verb, mediaType, None)

  def createControl(name: String, title: String, call: Call, mediaType: String) =
    createControl(name, title, call.url, call.method, mediaType)

  def createControl(name: String, title: String, call: Call, mediaType: String, schema: JsObject) =
    name -> HypermediaControl(title, call, mediaType, schema)

}


