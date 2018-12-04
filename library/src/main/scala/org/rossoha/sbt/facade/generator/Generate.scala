package org.rossoha.sbt.facade.generator

import java.nio.charset.StandardCharsets
import java.nio.file._

import io.circe.parser.decode
import org.rossoha.sbt.facade.generator.ComponentModel._
import org.rossoha.sbt.facade.generator.ComponentModelDecoders._
import org.rossoha.sbt.facade.generator.DocGenGen._

object Generate {

  def writeToFile(pathStr: String, filename: String, contents: String): Unit = {
    Files.createDirectories(Paths.get(pathStr))
    Files.write(Paths.get(pathStr.concat(filename)), contents.getBytes(StandardCharsets.UTF_8))
    ()
  }

  def main(args: Array[String]): Unit = {

    implicit val context = DocGenContext.MaterialUI

    def component(all: Map[String, Component],
                  path: String,
                  c: Component,
                  module: Option[String] = None): Unit = {
      val code    = genComponent(all, path, c, module)
      val name    = c.displayName
      val subPath = path.split('/').dropRight(1).mkString("/").toLowerCase
      code.foreach(
        s =>
          writeToFile(
            s"./scalajs-react-material-dashboard/js/src/main/scala/org/rebeam/mui/$subPath/",
            s"$name.scala",
            s
        )
      )
    }

    val s = scala.io.Source
      .fromInputStream(getClass.getResourceAsStream("/muidashboard.json"), "utf-8")
      .mkString

    println(s)

    val value = decode[Map[String, Component]](s)
    println(value)
    val d: Map[String, Component] = value.toOption.get

//     d.find(_._2.displayName == "Typography").foreach{
//       c => component(d, c._2)
//     }

    d.foreach {
      case (path, c) => component(d, path, c, Some("./material-kit-pro-react"))
    }

  }
}
