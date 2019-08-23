package models

case class NotFoundException(message: String = "") extends Exception
case class ForbiddenException(message: String = "") extends Exception
case class TaskNotArchivedException() extends Exception
case class BadRequestException(message: String = "") extends Exception