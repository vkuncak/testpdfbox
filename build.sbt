Compile / run / fork := true

name := "pdfviewer"

version := "0.1"

scalaVersion := "3.5.1"

libraryDependencies ++= Seq(
  "org.apache.pdfbox" % "pdfbox" % "2.0.29",
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  "com.fifesoft" % "rsyntaxtextarea" % "3.1.3"
)

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.5.1"

Compile / run / fork := true

