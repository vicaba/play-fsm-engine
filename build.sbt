name := "Play-FSM-Engine"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.8"

javacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-parameters",
  "-Xlint:unchecked",
  "-Xlint:deprecation"
)

crossScalaVersions := Seq("2.11.12", "2.12.7")

libraryDependencies += guice

libraryDependencies ++= Seq( ws )

// https://mvnrepository.com/artifact/org.apache.jena/apache-jena-libs
libraryDependencies += "org.apache.jena" % "apache-jena-libs" % "3.10.0" pomOnly()
