package com.allantl.atlassian.connect.http4s.domain

abstract class HostError(msg: String) extends Exception(msg)

case object HostNotFound extends HostError("Host not found")