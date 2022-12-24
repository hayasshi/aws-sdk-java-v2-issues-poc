import sbt._

object Dependencies {
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.3.5"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.14"

  lazy val awsSdk = "software.amazon.awssdk" % "dynamodb" % "2.19.4"

  lazy val testContainers = "org.testcontainers" % "testcontainers" % "1.17.6"
}
