package models

  case class NotFoundException() extends Exception
  case class ForbiddenException() extends Exception
  case class NotArchivedException() extends Exception