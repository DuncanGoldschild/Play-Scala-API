package controllers

import javax.inject.Inject
import models._
import play.api.mvc._
import utils.AppAction

import scala.concurrent._


class Schemas @Inject() (
                          components: ControllerComponents,
                          appAction: AppAction
                        ) extends AbstractController(components) {

  def createBoardSchema: Action[AnyContent] = Action.async { _ =>
    Future.successful(Ok(BoardCreationRequest.schema))
  }

  def authSchema: Action[AnyContent] = Action.async { _ =>
    Future.successful(Ok(Member.schema))
  }

  def updatePasswordMemberSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(MemberUpdateRequest.schema))
  }

  def createListSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(TasksListCreationRequest.schema))
  }

  def createTaskSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(TaskCreationRequest.schema))
  }

  def updateBoardSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(BoardUpdateRequest.schema))
  }

  def addDeleteMemberSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(MemberAddOrDelete.schema))
  }

  def updateLabelSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(LabelUpdateRequest.schema))
  }

  def descriptionUpdateSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(DescriptionUpdateRequest.schema))
  }

  def listIdOfTaskUpdateSchema: Action[AnyContent] = appAction.async { _ =>
    Future.successful(Ok(ListIdOfTaskUpdateRequest.schema))
  }
  //Todo : all other schemas and routes
}
