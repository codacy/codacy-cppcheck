import com.typesafe.sbt.packager.docker.Cmd

val cppcheckVersion: String = {
  val source = scala.io.Source.fromFile("Dockerfile")
  try {
    val prefix = "ARG toolVersion="
    source.getLines.find(_.startsWith(prefix)).get.stripPrefix(prefix)
  } finally {
    source.close()
  }
}

val commonSettings = Seq(scalaVersion := "2.13.3")

lazy val `doc-generator` = project
  .settings(
    commonSettings,
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "codacy" / "cppcheck" / "Versions.scala"
      IO.write(file, s"""package codacy.cppcheck
                        |object Versions {
                        |  val cppcheckVersion: String = "$cppcheckVersion"
                        |}
                        |""".stripMargin)
      Seq(file)
    }.taskValue,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "com.github.pathikrit" %% "better-files" % "3.8.0",
      "com.typesafe.play" %% "play-json" % "2.8.1"
    )
  )

val graalVersion = "20.1.0-java11"

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "codacy-cppcheck",
    libraryDependencies ++= Seq("com.codacy" %% "codacy-engine-scala-seed" % "5.0.1"),
    mainClass in Compile := Some("codacy.Engine"),
    graalVMNativeImageGraalVersion := Some(graalVersion),
    containerBuildImage := Some(s"oracle/graalvm-ce:$graalVersion"),
    graalVMNativeImageOptions ++= Seq(
      "-O1",
      "-H:+ReportExceptionStackTraces",
      "--no-fallback",
      "--no-server",
      "--initialize-at-build-time",
      "--report-unsupported-elements-at-runtime",
      "--static"
    )
  )
  .enablePlugins(GraalVMNativeImagePlugin)
