import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion := "2.13.3"

val cppcheckVersion: String = {
  val source = scala.io.Source.fromFile("Dockerfile.base")
  try {
    val prefix = "ARG toolVersion="
    source.getLines.find(_.startsWith(prefix)).get.stripPrefix(prefix)
  } finally {
    source.close()
  }
}

lazy val `doc-generator` = project
  .settings(
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
      "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
      "com.github.pathikrit" %% "better-files" % "3.9.2",
      "com.typesafe.play" %% "play-json" % "2.9.4"
    )
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "codacy-cppcheck",
    libraryDependencies ++= Seq("com.codacy" %% "codacy-engine-scala-seed" % "6.1.2"),
    mainClass in Compile := Some("codacy.Engine"),
    nativeImageOptions ++= List("-O1", "-H:+ReportExceptionStackTraces", "--no-fallback", "--no-server", "--static")
  )
  .enablePlugins(NativeImagePlugin)
  .enablePlugins(JavaAppPackaging)
