import Dependencies._

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.hayasshi"

lazy val root = (project in file("."))
  .settings(
    name := "aws-sdk-java-v2-issues-poc",
    libraryDependencies ++= Seq(
      logback,
      scalaTest      % Test,
      awsSdk         % Test,
      testContainers % Test
    )
  )
