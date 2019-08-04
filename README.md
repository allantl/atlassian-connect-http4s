Atlassian Connect Scala for Http4s
==================================

[![Build Status](https://travis-ci.org/allantl/atlassian-connect-http4s.svg?branch=master)](https://travis-ci.org/allantl/atlassian-connect-http4s)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.allantl/atlassian-connect-http4s_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.allantl/atlassian-connect-http4s_2.12)

This library serves as a starter to develop Atlassian Connect Jira and Confluence add on.
It is dependent on http4s which favors pure functional programming.

## Getting Started

Add this to your `build.sbt`:
```
scalacOptions += "-Ypartial-unification"
```

To add library dependencies:

```
libraryDependencies += "com.github.allantl" %% "atlassian-connect-http4s" % "0.1.0"
```

## Quick Start

This example is written using http4s version `0.20.0`.

1- Define lifecycle repository

You'll need to define an implementation to save and find Atlassian Host.
Note that, when add on is uninstalled, it is never deleted from storage but simply change the installed status.
In production, please use real database.

~~~ scala
import cats.effect.IO
import cats.effect.concurrent.Ref
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra

class AtlassianHostRepository(storage: Ref[IO, List[AtlassianHost]]) extends AtlassianHostRepositoryAlgebra[IO] {

  override def findByClientKey(clientKey: String, onlyInstalled: Boolean): IO[Option[AtlassianHost]] =
    if (onlyInstalled) {
      storage.get.map(_.find(h => h.clientKey == clientKey && h.installed))
    } else {
      storage.get.map(_.find(_.clientKey == clientKey))
    }

  override def findByBaseUrl(baseUrl: String, onlyInstalled: Boolean): IO[Option[AtlassianHost]] =
    if (onlyInstalled) {
      storage.get.map(_.find(h => h.baseUrl == baseUrl && h.installed))
    } else {
      storage.get.map(_.find(_.baseUrl == baseUrl))
    }

  override def save(atlassianHost: AtlassianHost): IO[AtlassianHost] =
    storage
      .update(atlassianHost :: _.filter(_.clientKey == atlassianHost.clientKey))
      .map(_ => atlassianHost)
}
~~~

2- Define your service endpoints

Use `AcHttpRoutes` to define your endpoints and `asAcAuth` to get the authenticated user.

`import com.allantl.atlassian.connect.http4s._` must be in scope.

~~~ scala
import cats.effect.IO
import com.allantl.atlassian.connect.http4s._
import org.http4s.dsl.Http4sDsl

class AcServiceEndpoints extends Http4sDsl[IO] {

  val endpoints = AcHttpRoutes.of[IO] {
    case GET -> Root / "ping" asAcAuth user =>
      Ok(s"Received response from ${user.host.baseUrl}")
  }
}
~~~

3- Initiliaze components

~~~ scala
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.JwtValidator
import com.allantl.atlassian.connect.http4s.auth.middleware.AcHttpService
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import com.allantl.atlassian.connect.http4s.endpoints.LifecycleEndpoints
import com.allantl.atlassian.connect.http4s.services.lifecycle.LifecycleService
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    Ref.of[IO, List[AtlassianHost]](List.empty).flatMap { ref =>

      implicit val atlassianHostRepo = new AtlassianHostRepository(ref)
      implicit val jwtValidator = new JwtValidator[IO]()

      // AcHttpService is needed to transform `AcHttpRoutes` into http4s `HttpRoutes`
      val acHttpService: AcHttpService[IO] = AcHttpService(jwtValidator)
      val myService = acHttpService.liftRoutes(new AcServiceEndpoints().endpoints)

      val lifecycleService = new LifecycleService[IO](atlassianHostRepo, infoLogger = log => IO.delay(println(log)))
      val lifecycleEndpoints = LifecycleEndpoints(jwtValidator, atlassianHostRepo, lifecycleService).endpoints

      // It is recommended to split lifecycle to different routes
      val httpApp = Router(
        "/api/lifecycle" -> lifecycleEndpoints,
        "/api" -> myService
      ).orNotFound

      BlazeServerBuilder[IO]
        .bindHttp(8080, "localhost")
        .withHttpApp(httpApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}
~~~

## Providing your own LifecycleService

You can take a look at the implementation [here](src/main/scala/com/allantl/atlassian/connect/http4s/services/lifecycle/LifecycleService.scala) and roll your own service instead.

## Perform operation after lifecycle events

You can provide your own implementation of `LifecycleEventHandler` when initializing `LifecycleEndpoints`.
Method will run asynchronously in the background and will not block lifecycle event. 

## Composing Routes

`AcHttpRoutes` can be composed with other `AcHttpRoutes`.

Make sure you do not compose this with `Http4sRoutes`, since `AcHttpRoutes` needs authentication.

~~~ scala
import cats.implicits._

val e1 = new AcServiceEndpoints()
val e2 = new AcServiceEndpoints()

val e3 = e1.endpoints <+> e2.endpoints
~~~

## License Check

There is a middleware that handles license check.

~~~ scala
import cats.effect._
import org.http4s.dsl.io._

// For development, you can set this to false
implicit val atlassianConnectConfig = AtlassianConnectConfig(licenseCheckEnabled = true)

val notLicensedEndpoints: Request[IO] => IO[Response[IO]] = _ => Ok("License not active")
val licenseCheck: LicenseCheck[IO] = new LicenseCheck[IO](notLicensedEndpoints)

val endpoints: HttpRoutes[IO] = ???
val licensedEndpoints = licenseCheck(endpoints)
~~~

## Serving frontend assets

To render frontend html page, you can use http4s with twirl, take a look at the [documentation](https://http4s.org/v0.20/entity/).

## Frontend JWT Authentication

This is explained in atlassian connect [documentation](https://developer.atlassian.com/cloud/jira/platform/cacheable-app-iframes/),
under Retrieving context using AP.context.getToken().

## Jira Client

If you need jira client, its available [here](https://github.com/allantl/jira4s).

Interop with this library:

~~~ scala
object JiraClient {

  type JiraClient[R[_]] = JiraMultiTenantClient[R]

  def apply[R[_], S](acJwtConfig: AcJwtConfig)(
    implicit sttpBackend: SttpBackend[R, S]
  ): JiraMultiTenantClient[R] =
    JiraMultiTenantClient(acJwtConfig)
}
~~~

## ScalaJs Atlaskit components

For those who is developing with scalajs-react and is looking for atlaskit components, please take a look [here](https://github.com/allantl/scalajs-atlaskit)
