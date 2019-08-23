package hypermedia

import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json, Writes}

case class Resource[A](
                        value: Option[A],
                        id: Option[String],
                        embedded: List[HypermediaControl.EmbeddedEntity] = List(),
                        controls: Map[String, HypermediaControl] = Map()
                      ) {

  def this(selfControl: (String, HypermediaControl)) =
    this(None, None, controls = Map(selfControl))

  def this(controls: Map[String, HypermediaControl]) =
    this(None, None, controls = controls)

  def this(value: A, controls: Map[String, HypermediaControl]) =
    this(Some(value), None, controls = controls)

  def this(id: String, controls: Map[String, HypermediaControl]) =
    this(None, Some(id), controls =  controls)

  def this(value: A, embedded: List[HypermediaControl.EmbeddedEntity], controls: Map[String, HypermediaControl]) =
    this(Some(value), None, embedded, controls)

  def toJson(implicit writes: Writes[A]): JsValue = Json.toJson(this)(writes())

  implicit def writes(implicit writesA: Writes[A]): Writes[Resource[A]] =
    (resource: Resource[A]) =>
      JsObject(
        List(
          resource.value.map("info" -> Json.toJson(_)),
          resource.id.map("id" -> JsString(_)),
          toJson(resource.embedded).map { "elements" -> _ },
          toJson(resource.controls).map { "@controls" -> _ }
      ).flatten)


  implicit val writesReference: Writes[Resource[Reference]] = (resource: Resource[Reference]) =>
    JsObject(
      List(
        resource.value.map(v => "id" -> JsString(v.id)),
        resource.value.map(v => "label" -> JsString(v.label)),
        toJson(resource.controls).map { "@controls" -> _ }
      ).flatten)

  private def toJson(entry: Map[String, HypermediaControl]): Option[JsValue] =
    if (entry.isEmpty)
      None
    else
      Some(JsObject(entry.toList.map {
        case (name: String, control: HypermediaControl) => name -> control.toJson
      }))

  private def toJson(entry: List[HypermediaControl.EmbeddedEntity]): Option[JsValue] =
    if (entry.isEmpty)
      None
    else
      Some(JsArray(entry.map { Json.toJson(_)} ))

}
