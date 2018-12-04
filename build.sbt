import Publishing._
import sbt._

lazy val root = (project in file("."))
  .settings(ReleaseSettings: _*)
  .aggregate(plugin, library)
  .settings(
   
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(PublishSettings: _*)
  .settings(
    sbtPlugin := true,
    name := "react-facade-generator",
    crossSbtVersions := Vector("1.0.3", "1.2.7"),
    scriptedBufferLog := false,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    }
  )
  .dependsOn(library)

lazy val library = (project in file("library"))
  .settings(LibraryPublishSettings: _*)
  .settings(
    name := "react-facade-generator-lib",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % "0.10.1",
      "io.circe" %% "circe-generic" % "0.10.1",
      "io.circe" %% "circe-parser"  % "0.10.1",
      "org.scalatest" %% "scalatest" % "3.0.0" % Test
    )
  )
