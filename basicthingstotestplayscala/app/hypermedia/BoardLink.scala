package hypermedia

import play.api.libs.json.Writes
import play.api.libs.json.JsValue
import play.api.mvc.Call
import play.api.libs.json.Json

case class BoardLink(rel: Rel, uri: String, mediaType: String, id: String, label: String, displayFormat: String) {

  def withRel(rel: String, method: String): BoardLink = {
    withRel(OTHER(rel, method))
  }

  def withSelfRel: BoardLink = {
    withRel(SELF)
  }

  def withGetRel: BoardLink = {
    withRel(GET)
  }

  def withUpdateRel: BoardLink = {
    withRel(UPDATE)
  }

  def withCreateRel: BoardLink = {
    withRel(CREATE)
  }

  def withDeleteRel: BoardLink = {
    withRel(DELETE)
  }

  def withRel(rel: Rel): BoardLink = this.copy(rel = rel)


  def withJsonMediaType: BoardLink = {
    withMediaType("application/json")
  }

  def withMediaType(mediaType: String): BoardLink = {
    this.copy(mediaType = mediaType)
  }

  def withId(id: String): BoardLink = {
    this.copy(id = id)
  }

  def withLabel(label: String): BoardLink = {
    this.copy(label = label)
  }

  def withDisplayFormat(displayFormat: String): BoardLink = {
    this.copy(displayFormat = displayFormat)
  }
}


object BoardLink {

  implicit val boardLinkWrites = new Writes[BoardLink] {
    def writes(l: BoardLink): JsValue = {
      l.displayFormat match {
        case "GetOneElement" =>
          Json.obj(
              "id" -> l.id,
              "label" -> l.label,
              "@controls" -> Json.obj(
                l.rel.name -> Json.obj(
                "href" -> l.uri,
                "verb" -> l.rel.method,
                "mediaType" -> l.mediaType
              )
            )
          )
        case "MethodOnSelf" =>
          Json.obj(
            l.rel.name -> Json.obj(
              "href" -> l.uri,
              "verb" -> l.rel.method,
              "mediaType" -> l.mediaType
            )
          )
        case _ =>
          Json.obj(
            l.rel.name -> Json.obj(
              "href" -> l.uri,
              "verb" -> l.rel.method,
              "mediaType" -> l.mediaType
            )
          )
      }
    }
  }


  def linkTo(call: Call): BoardLink = {
    BoardLink(NONE, call.toString, "", "", "", "")
  }

  def linkTo(uri: String): BoardLink = {
    BoardLink(NONE, uri, "", "", "", "")
  }
}