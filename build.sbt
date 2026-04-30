import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion := "2.13.16"

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
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
      "com.github.pathikrit" %% "better-files" % "3.9.2",
      "org.playframework" %% "play-json" % "3.0.5"
    )
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "codacy-cppcheck",
    libraryDependencies ++= Seq("com.codacy" %% "codacy-engine-scala-seed" % "6.1.5"),
    mainClass in Compile := Some("codacy.Engine"),
    nativeImageOptions ++= List("-O1", "-H:+ReportExceptionStackTraces", "--no-fallback", "--no-server", "--static"),
    nativeImageVersion := "22.3.1",
    nativeImageJvm := "graalvm-java17"
  )
  .enablePlugins(NativeImagePlugin)
  .enablePlugins(JavaAppPackaging)
