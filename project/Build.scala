import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "LODE"
  val appVersion      = "1.0"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "commons-lang" % "commons-lang" % "2.3"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    javacOptions += "-Xlint:deprecation"
          
  )

}
