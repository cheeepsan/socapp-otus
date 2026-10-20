ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "socapp"

lazy val tapirVersion = "1.11.15"
lazy val http4sVersion = "0.23.30"
lazy val doobieVersion = "1.0.0-RC5"
lazy val circeVersion = "0.14.10"
lazy val catsEffectVersion = "3.5.7"

lazy val root = (project in file("."))
  .settings(
    name := "socapp",
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Wunused:all"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.postgresql" % "postgresql" % "42.7.4",
      "org.mindrot" % "jbcrypt" % "0.4",
      "ch.qos.logback" % "logback-classic" % "1.3.14",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "com.typesafe" % "config" % "1.4.3",
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
    )
  )
