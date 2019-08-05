package hypermedia

sealed trait Rel { def name: String; def method: String }
case object NONE extends Rel { val name = ""; val method = "" }
case object SELF extends Rel { val name = "self"; val method = "GET" }
case object UPDATE extends Rel { val name = "update"; val method = "PUT" }
case object GET extends Rel { val name = "get"; val method = "GET" }
case object DELETE extends Rel { val name = "delete"; val method = "DELETE" }
case object CREATE extends Rel { val name = "create"; val method = "POST" }
case class OTHER(rel: String, method: String) extends Rel { val name: String = rel; val httpMethod: String = method }
