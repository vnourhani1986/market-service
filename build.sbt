name := "market.service"

version := "1.0"
scalaVersion := "2.12.7"

organization := "com.snapptrip"

// To resolve  prometheus dependencies we need artima maven repo
resolvers ++= Seq(
  Resolver.bintrayRepo("lonelyplanet", "maven"),
  "Artima Maven Repository" at "http://repo.artima.com/releases"

)

libraryDependencies ++= {
  val akkaVersion       = "2.5.13"
  val akkaHttpVersion   = "10.1.3"
  val scalaTestVersion  = "3.0.5"
  val slickVersion      = "3.2.1"
  val postgresVersion   = "42.1.4"
  val scalazVersion     = "7.2.25"
  Seq(
    "com.typesafe.akka"           %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka"           %% "akka-actor"           % akkaVersion,
    "com.typesafe.akka"           %% "akka-stream"          % akkaVersion,

    // Test libs
    "com.typesafe.akka"           %% "akka-http-testkit"    % akkaHttpVersion   % Test,
    "com.typesafe.akka"           %% "akka-testkit"         % akkaVersion       % Test,
    "org.scalatest"               %% "scalatest"            % scalaTestVersion  ,
    "org.scalamock"               %% "scalamock-scalatest-support" % "3.6.0"    % Test,
    "org.scalamock"               %% "scalamock"            % "4.1.0"           % Test,

    // Database libs
    "com.typesafe.slick"          %% "slick"                % slickVersion,
    "com.typesafe.slick"          %% "slick-hikaricp"       % slickVersion,
    "org.postgresql"               % "postgresql"           % postgresVersion,
    "com.github.tminglei"         %% "slick-pg_spray-json"  % "0.16.3",
    "com.github.tminglei"         %% "slick-pg"             % "0.16.3",
    // Logging libs
    "com.typesafe.akka"           %% "akka-slf4j"           % "2.4.12",
    "ch.qos.logback"              %  "logback-classic"      % "1.1.3",
    "com.typesafe.scala-logging"  %% "scala-logging"        % "3.7.2",

    // Spray Json
    "com.typesafe.akka"           %% "akka-http-spray-json" % "10.1.5",
    "com.github.etaty"            %% "rediscala"            % "1.8.0",

    // JWT + spray integration
    "com.pauldijou"               %% "jwt-spray-json"       % "0.19.0",

    // xml
    "org.codehaus.plexus"         % "plexus-utils"          % "3.1.0",
    "com.typesafe.akka"           %% "akka-http-xml"        % "10.1.5",

    //scalaz
    "org.scalaz"                  %% "scalaz-core"          % scalazVersion,
    "com.lihaoyi"                 %% "pprint"               % "0.5.3",
    "com.github.etaty"            %% "rediscala"            % "1.8.0",

    //sentry
    "io.sentry"                   % "sentry-logback"        % "1.7.15",

    // xls apache poi
    "org.apache.poi"              % "poi"                   % "3.9",

    // Migration for SQL databases
    "org.flywaydb"                % "flyway-core"           % "4.2.0",

    // stream slick (jdbc)
    "com.lightbend.akka"          %% "akka-stream-alpakka-slick" % "1.0-M1",

    // jalcal converter
    "com.github.sbahmani"         % "jalcal"                % "1.4"

  )
}



Revolver.settings
enablePlugins(JavaServerAppPackaging)