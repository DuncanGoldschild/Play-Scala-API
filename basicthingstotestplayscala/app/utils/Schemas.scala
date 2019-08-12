package utils


import javax.inject.Inject
import models.{BoardCreationRequest, BoardUpdateRequest, MemberAddOrDelete}

import scala.concurrent._
import play.api.mvc._
import play.api.libs.json._


class Schemas @Inject() (
                          components: ControllerComponents,
                          appAction: AppAction
                        ) extends AbstractController(components) {

  def createBoardSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(BoardCreationRequest.schema))
  }

  def updateBoardSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(BoardUpdateRequest.schema))
  }

  def addMemberBoardSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(MemberAddOrDelete.schema))
  }

  def deleteMemberBoardSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(MemberAddOrDelete.schema))
  }

  //Todo : all other schemas and routes
}
