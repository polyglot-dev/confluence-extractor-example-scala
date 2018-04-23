
name := "confluence-extractor-example-scala"

version := "0.1"

scalaVersion := "2.12.5"

lazy val akkaVersion = "2.5.12"
lazy val akkaHttpVersion = "10.1.1"

//libraryDependencies += "io.vertx" %% "vertx-lang-scala" % "3.4.1"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion
  , "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  , "com.typesafe.akka" %% "akka-stream" % akkaVersion

  , "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  , "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  , "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion

  , "org.json4s" %% "json4s-native" % "3.5.3"
  , "org.json4s" %% "json4s-jackson" % "3.5.3"

  , "org.scala-lang.modules" %% "scala-async" % "0.9.7"

)

addCommandAlias("uc", ";updateClassifiers")
addCommandAlias("ucr", ";updateClassifiers;run")
addCommandAlias("r", ";run")
addCommandAlias("c", ";compile")
addCommandAlias("rl", ";reload")
addCommandAlias("rlc", ";reload;clean")

