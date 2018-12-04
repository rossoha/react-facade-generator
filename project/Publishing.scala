import sbt.Keys.{scalaVersion, _}
import sbt._
import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, _}
import sbtrelease.ReleaseStateTransformations._
object Publishing {

  private val CommonSettings = Seq(
    organization := "org.rossoha.sbt",
      scalaVersion := "2.12.7"
  )

  /* `publish` performs a no-op */
  val NoopPublishSettings = CommonSettings ++ Seq(
    packagedArtifacts in RootProject(file(".")) := Map.empty,
    publish := Unit,
    publishLocal := Unit,
    publishArtifact := false,
    publishTo := None
  )

  val PublishSettings = CommonSettings ++ Seq(
    autoAPIMappings := true,
    pomIncludeRepository := { _ => false
    },
    publishArtifact in Test := false,
    publishArtifact in (Compile, packageDoc) := true,
    publishArtifact in (Compile, packageSrc) := true
  )

  val ReleaseSettings = NoopPublishSettings ++ Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publish"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

  val LibraryPublishSettings = PublishSettings ++ Seq(
//  bintrayRepository := "maven",
//  bintrayPackage := "sbt-tweeter-libs",
    publishMavenStyle := true
//  homepage := Some(new URL("https://github.com/Tapad/sbt-tweeter")),
//  pomExtra := {
//    <developers>
//      <developer>
//        <id>jeffreyolchovy</id>
//        <name>Jeffrey Olchovy</name>
//        <email>jeffo@tapad.com</email>
//        <url>https://github.com/jeffreyolchovy</url>
//      </developer>
//    </developers>
//      <scm>
//        <url>https://github.com/Tapad/sbt-tweeter</url>
//        <connection>scm:git:git://github.com/Tapad/sbt-tweeter.git</connection>
//      </scm>
//  }
  )

}
