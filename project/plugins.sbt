resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.5")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

resolvers += Resolver.sonatypeRepo("public")