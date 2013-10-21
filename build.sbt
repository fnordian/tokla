name := "token"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "org.squeryl" % "squeryl_2.10" % "0.9.5-6",
  cache
)     

play.Project.playScalaSettings
