package com.allantl.atlassian.connect.http4s

import com.allantl.atlassian.connect.http4s.gen.AtlassianHostGen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

trait AcHttp4sTest extends Specification with AtlassianHostGen with ScalaCheck
