package codacy.cppcheck

import com.codacy.plugins.api.results.Result.Issue
import com.codacy.plugins.api.results.{Pattern, Result, Tool}
import com.codacy.plugins.api.{Options, Source}
import com.codacy.tools.scala.seed.utils.CommandRunner
import play.api.libs.json._

import java.nio.file.Files
import scala.util.Try

case class WarnResult(patternId: String, file: String, message: String, line: String = "1")

object WarnResult {
  implicit val warnResultFmt = Json.format[WarnResult]
}

object CPPCheck extends Tool {

  lazy val blacklist = Set("unusedFunction")

  private val languageConfKey = Options.Key("language")

  override def apply(
      source: Source.Directory,
      configuration: Option[List[Pattern.Definition]],
      files: Option[Set[Source.File]],
      options: Map[Options.Key, Options.Value]
  )(implicit specification: Tool.Specification): Try[List[Result]] = {
    Try {
      val filesToLint: Seq[String] = files.fold(Seq(source.path)) { paths =>
        paths.map(_.toString).toSeq
      }

      val languageParameter: String = options
        .get(languageConfKey)
        .flatMap { language =>
          Option(language: JsValue).collect {
            case JsString(lang) => s"--language=$lang"
          }
        }
        .getOrElse("")

      val tempFolder = Files.createTempDirectory("cppcheck-build-dir-")
      tempFolder.toFile().deleteOnExit()

      def addonIfNeeded(name: String, parameter: Option[String] = None): Option[String] = {
        val enabled = configuration match {
          case Some(patterns) => patterns.exists(_.patternId.value.startsWith(s"$name-"))
          case None => true
        }
        if (enabled) Some(s"--addon=${parameter.getOrElse(name)}")
        else None
      }

      val command: List[String] = List("cppcheck", "--enable=all") ++
        addonIfNeeded("cert") ++
        addonIfNeeded("y2038") ++
        addonIfNeeded("threadsafety") ++
        addonIfNeeded("misra", Some("addons/misra.json")) ++
        List(
          s"--cppcheck-build-dir=${tempFolder.toString}",
          "--error-exitcode=0",
          "--inline-suppr",
          "--force",
          s"-j ${Runtime.getRuntime().availableProcessors()}",
          languageParameter,
          """--template={"patternId":"{id}","file":"{file}","line":"{line}","message":"{message}"}"""
        ) ++
        filesToLint

      CommandRunner.exec(command) match {
        case Right(resultFromTool) =>
          val output = resultFromTool.stdout ++ resultFromTool.stderr
          parseToolResult(output, checkPattern(configuration)).filterNot(blacklisted)
        case Left(failure) =>
          throw failure
      }
    }
  }

  private def parseToolResult(resultFromTool: List[String], wasRequested: String => Boolean): List[Result] = {
    for {
      outputLine <- resultFromTool
      result <- Try(Json.parse(outputLine)).toOption.flatMap(_.asOpt[WarnResult]) if wasRequested(result.patternId)
      line <- Try(result.line.toInt).toOption
    } yield
      Issue(Source.File(result.file), Result.Message(result.message), Pattern.Id(result.patternId), Source.Line(line))
  }

  private def checkPattern(conf: Option[List[Pattern.Definition]])(patternId: String): Boolean = {
    conf.forall(_.exists(_.patternId.value.toLowerCase == patternId.toLowerCase))
  }

  def blacklisted(result: Result): Boolean = {
    result match {
      case issue: Issue => blacklist.contains(issue.patternId.value)
      case _ => false
    }
  }

}
