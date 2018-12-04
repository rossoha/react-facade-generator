package org.rossoha.sbt.facade
package generator

import sbt.Def.Initialize
import sbt.Keys.scalaInstance
import sbt.ScriptedPlugin.autoImport.scriptedClasspath
import sbt._
import sbt.internal.inc.ModuleUtilities
import sbt.internal.inc.classpath.ClasspathUtilities
import sbt.plugins.JvmPlugin

object FacadeGeneratorKeys {
  val facadeJsModule =
    settingKey[String]("A js module path. This value will be add to @JSImport() annotation.")
  val facadeBasePackage = settingKey[String]("A package for generated classes.")
  val facadeMetadataFile =
    settingKey[String]("A js module path. This value will be add to @JSImport() annotation.")
  var makeFacade = taskKey[Unit]("A task that is automatically imported to the build")
}

object FacadeGeneratorPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    val FacadeGeneratorKeys           = generator.FacadeGeneratorKeys
    val facadeBasePackage             = FacadeGeneratorKeys.facadeBasePackage
    val facadeJsModule                = FacadeGeneratorKeys.facadeJsModule
    val facadeMetadataFile            = FacadeGeneratorKeys.facadeMetadataFile
    val makeFacade                    = FacadeGeneratorKeys.makeFacade
    val facadeMetadataFileDefaultName = "components_metadata"
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    // Required settings that must be defined by the project utilizing the plugin
    facadeBasePackage := {
      facadeBasePackage.??(undefinedKeyError(facadeBasePackage.key)).value
    },
    facadeJsModule := {
      facadeJsModule.??(undefinedKeyError(facadeJsModule.key)).value
    },
    facadeMetadataFile := {
      facadeMetadataFile.??(facadeMetadataFileDefaultName).value
    },
    /* The one input task that will be available to our plugin users, by default */
    makeFacade := {
      println("== Facade settings ==")
      println("Facade base package: " + facadeBasePackage.value)
      println("Facade JS module: " + facadeJsModule.value)
      println("Facade metadata file: " + facadeMetadataFile.value)

    }
  )

  private def undefinedKeyError[A](key: AttributeKey[A]): A =
    sys.error(
      s"${key.description.getOrElse("A required key")} is not defined. " +
        s"Please declare a value for the `${key.label}` key."
    )

  private[generator] def scriptedTestsTask: Initialize[Task[AnyRef]] =
    Def.task {
      val loader = ClasspathUtilities.toLoader(scriptedClasspath.value, scalaInstance.value.loader)
      try {
        ModuleUtilities.getObject("sbt.scriptedtest.ScriptedTests", loader)
      } catch {
        case _: ClassNotFoundException =>
          ModuleUtilities.getObject("sbt.test.ScriptedTests", loader)
      }
    }
}
