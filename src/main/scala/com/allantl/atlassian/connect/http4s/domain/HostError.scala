package com.allantl.atlassian.connect.http4s.domain

sealed trait HostError {
  def msg: String
}

case object HostNotFound extends HostError {
  override val msg = "Host not found"
}
