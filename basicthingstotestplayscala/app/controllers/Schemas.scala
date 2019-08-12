package controllers

import javax.inject.Inject
import models._
import play.api.libs.json._
import play.api.mvc._
import utils.AppAction

import scala.concurrent._


class Schemas @Inject() (
                          components: ControllerComponents,
                          appAction: AppAction
                        ) extends AbstractController(components) {

  def createBoardSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(BoardCreationRequest.schema))
  }

  def createListSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(TasksListCreationRequest.schema))
  }

  def createTaskSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(TaskCreationRequest.schema))
  }

  def updateBoardSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(BoardUpdateRequest.schema))
  }

  def addDeleteMemberSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(MemberAddOrDelete.schema))
  }

  def updateLabelSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(LabelUpdateRequest.schema))
  }

  def descriptionUpdateSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(DescriptionUpdateRequest.schema))
  }

  def listIdOfTaskUpdateSchema: Action[JsValue] = appAction.async(parse.json) { _ =>
    Future.successful(Ok(ListIdOfTaskUpdateRequest.schema))
  }
  //Todo : all other schemas and routes
}
