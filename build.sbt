Compile / run / fork := true

// build.sbt
lazy val root = (project in file("."))
  .settings(
    name := "pdfviewer",
    version := "0.1",
    scalaVersion := "3.5.1",
    libraryDependencies ++= Seq(
      "org.apache.pdfbox" % "pdfbox" % "2.0.29",
      "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
      "com.fifesoft" % "rsyntaxtextarea" % "3.1.3",
    ),
    assembly / mainClass := Some("pdfview.Main")
  )
