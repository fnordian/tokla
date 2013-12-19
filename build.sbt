name := "token"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "org.squeryl" % "squeryl_2.10" % "0.9.5-6",
  cache,
  "javax.mail" % "mail" % "1.4.7",
  "cglib" % "cglib" % "3.0",
  "de.bripkens" % "gravatar4java" % "1.1",
  "org.scala-lang" % "scala-actors" % "2.10.0"
)     

play.Project.playScalaSettings

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-Xmax-classfile-name", "128")
