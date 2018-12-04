import sbt._


Option(System.getProperty("plugin.version")) match {
  case None =>
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  case Some(pluginVersion) =>
    addSbtPlugin("org.rossoha.sbt" % """react-facade-generator""" % pluginVersion)

}
