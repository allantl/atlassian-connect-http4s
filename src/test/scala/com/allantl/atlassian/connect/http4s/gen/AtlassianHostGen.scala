package com.allantl.atlassian.connect.http4s.gen

import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import org.scalacheck._

trait AtlassianHostGen {

  def atlassianHostGen(baseUrl: String): Gen[AtlassianHost] =
    for {
      clientKey <- Gen.alphaStr.suchThat(_.nonEmpty)
      key <- Gen.alphaStr.suchThat(_.nonEmpty)
      publicKey <- Gen.alphaStr.suchThat(_.nonEmpty)
      oauthClientId <- Gen.uuid.map(_.toString)
      sharedSecret <- Gen.uuid.map(_.toString)
    } yield
      AtlassianHost(
        clientKey,
        key,
        publicKey,
        Some(oauthClientId),
        sharedSecret,
        "1.0",
        "1.0",
        baseUrl,
        "Jira",
        "Atlassian Connect testing",
        None,
        installed = true
      )

}
