package codacy.cppcheck

import play.api.libs.json.{JsArray, Json}
import better.files._
import better.files

import scala.xml.{Elem, XML}
import scala.sys.process._

object DocGenerator {

  case class Ruleset(patternId: String, level: String, title: String, description: String)

  def main(args: Array[String]): Unit = {
    val rules = (for {
      file <- File.temporaryFile()
      javaFile = file.toJava
      _ = assert("docker run -i --entrypoint cppcheck codacy-cppcheck:latest --errorlist".#>(javaFile).! == 0)
      res = getRules(javaFile)
    } yield res).get()
    createPatternsAndDescriptionFile(rules)
  }

  private def generatePatterns(rules: Seq[Ruleset]): JsArray = {
    val defaultPatterns = Set(
      "AssignmentAddressToInteger",
      "AssignmentIntegerToAddress",
      "CastAddressToIntegerAtReturn",
      "CastIntegerToAddressAtReturn",
      "ConfigurationNotChecked",
      "IOWithoutPositioning",
      "StlMissingComparison",
      "arithOperationsOnVoidPointer",
      "arrayIndexOutOfBounds",
      "arrayIndexOutOfBoundsCond",
      "arrayIndexThenCheck",
      "assertWithSideEffect",
      "assignBoolToPointer",
      "assignIfError",
      "assignmentInAssert",
      "autoVariables",
      "autovarInvalidDeallocation",
      "badBitmaskCheck",
      "boostForeachError",
      "bufferAccessOutOfBounds",
      "catchExceptionByValue",
      "charBitOp",
      "charLiteralWithCharPtrCompare",
      "checkCastIntToCharAndBack",
      "clarifyCalculation",
      "clarifyCondition",
      "clarifyStatement",
      "commaSeparatedReturn",
      "compareBoolExpressionWithInt",
      "comparisonError",
      "comparisonFunctionIsAlwaysTrueOrFalse",
      "comparisonOfBoolWithBoolError",
      "comparisonOfFuncReturningBoolError",
      "comparisonOfTwoFuncsReturningBoolError",
      "constStatement",
      "copyCtorPointerCopying",
      "coutCerrMisusage",
      "cstyleCast",
      "deallocDealloc",
      "deallocret",
      "deallocuse",
      "derefInvalidIterator",
      "doubleFree",
      "duplInheritedMember",
      "duplicateBreak",
      "duplicateExpression",
      "duplicateExpressionTernary",
      "eraseDereference",
      "exceptDeallocThrow",
      "exceptRethrowCopy",
      "exceptThrowInDestructor",
      "fflushOnInputStream",
      "ignoredReturnValue",
      "incorrectLogicOperator",
      "incorrectStringBooleanError",
      "incorrectStringCompare",
      "incrementboolean",
      "integerOverflow",
      "invalidFunctionArg",
      "invalidFunctionArgBool",
      "invalidIterator1",
      "invalidLengthModifierError",
      "invalidPointerCast",
      "invalidPrintfArgType_float",
      "invalidPrintfArgType_n",
      "invalidPrintfArgType_p",
      "invalidPrintfArgType_s",
      "invalidPrintfArgType_sint",
      "invalidPrintfArgType_uint",
      "invalidScanfArgType_float",
      "invalidScanfArgType_int",
      "invalidScanfArgType_s",
      "invalidScanfFormatWidth",
      "invalidscanf",
      "knownConditionTrueFalse",
      "leakNoVarFunctionCall",
      "leakReturnValNotUsed",
      "literalWithCharPtrCompare",
      "mallocOnClassError",
      "mallocOnClassWarning",
      "memleak",
      "memleakOnRealloc",
      "memsetClass",
      "memsetClassFloat",
      "memsetClassReference",
      "memsetFloat",
      "memsetValueOutOfRange",
      "memsetZeroBytes",
      "mismatchAllocDealloc",
      "mismatchSize",
      "mismatchingBitAnd",
      "mismatchingContainers",
      "missingInclude",
      "missingIncludeSystem",
      "moduloAlwaysTrueFalse",
      "multiCondition",
      "nanInArithmeticExpression",
      "negativeIndex",
      "noConstructor",
      "noCopyConstructor",
      "noExplicitConstructor",
      "nullPointer",
      "nullPointerDefaultArg",
      "nullPointerRedundantCheck",
      "operatorEq",
      "operatorEqMissingReturnStatement",
      "operatorEqRetRefThis",
      "operatorEqShouldBeLeftUnimplemented",
      "operatorEqToSelf",
      "operatorEqVarError",
      "oppositeInnerCondition",
      "passedByValue",
      "pointerArithBool",
      "pointerLessThanZero",
      "pointerOutOfBounds",
      "pointerPositive",
      "pointerSize",
      "postfixOperator",
      "preprocessorErrorDirective",
      "publicAllocationError",
      "raceAfterInterlockedDecrement",
      "readWriteOnlyFile",
      "redundantAssignInSwitch",
      "redundantAssignment",
      "redundantCondition",
      "redundantCopy",
      "redundantCopyInSwitch",
      "redundantIfRemove",
      "redundantPointerOp",
      "resourceLeak",
      "returnAddressOfAutoVariable",
      "returnAddressOfFunctionParameter",
      "returnLocalVariable",
      "returnReference",
      "returnTempReference",
      "seekOnAppendedFile",
      "selfAssignment",
      "selfInitialization",
      "shiftNegative",
      "shiftTooManyBits",
      "signConversion",
      "sizeofCalculation",
      "sizeofDereferencedVoidPointer",
      "sizeofDivisionMemfunc",
      "sizeofVoid",
      "sizeofsizeof",
      "sizeofwithnumericparameter",
      "sizeofwithsilentarraypointer",
      "sprintfOverlappingData",
      "staticStringCompare",
      "stlBoundaries",
      "stlIfFind",
      "stlIfStrFind",
      "stlOutOfBounds",
      "stlSize",
      "stlcstr",
      "stlcstrParam",
      "stlcstrReturn",
      "strPlusChar",
      "stringCompare",
      "stringLiteralWrite",
      "thisSubtraction",
      "throwInNoexceptFunction",
      "toomanyconfigs",
      "truncLongCastAssignment",
      "truncLongCastReturn",
      "unassignedVariable",
      "uninitMemberVar",
      "uninitStructMember",
      "uninitdata",
      "uninitstring",
      "uninitvar",
      "unpreciseMathCall",
      "unreachableCode",
      "unreadVariable",
      "unsafeClassCanLeak",
      "unsignedLessThanZero",
      "unsignedPositive",
      "unusedAllocatedMemory",
      "unusedLabel",
      "unusedScopedObject",
      "unusedStructMember",
      "useClosedFile",
      "useInitializationList",
      "uselessAssignmentArg",
      "uselessAssignmentPtrArg",
      "uselessCallsCompare",
      "uselessCallsEmpty",
      "uselessCallsRemove",
      "uselessCallsSubstr",
      "uselessCallsSwap",
      "va_end_missing",
      "va_list_usedBeforeStarted",
      "va_start_referencePassed",
      "va_start_subsequentCalls",
      "va_start_wrongParameter",
      "varFuncNullUB",
      "variableScope",
      "virtualDestructor",
      "writeReadOnlyFile",
      "wrongPipeParameterSize",
      "wrongPrintfScanfArgNum",
      "wrongPrintfScanfParameterPositionError",
      "wrongmathcall",
      "zerodiv",
      "zerodivcond"
    )

    val codacyPatterns = rules.map { rule =>
      val category: String =
        if (rule.level.startsWith("error") ||
          rule.level.startsWith("warning") ||
          rule.level.startsWith("portability")) {
          "ErrorProne"
        } else if (rule.level.startsWith("performance")) {
          "Performance"
        } else {
          "CodeStyle"
        }

      val level: String =
        if (rule.level.startsWith("warning") ||
          rule.level.startsWith("portability") ||
          rule.level.startsWith("performance")) {
          "Warning"
        } else if (rule.level.startsWith("error")) {
          "Error"
        } else {
          "Info"
        }

      Json.obj(
        "patternId" -> rule.patternId,
        "level" -> level,
        "category" -> category,
        "enabled" -> defaultPatterns.contains(rule.patternId)
      )

    }
    Json.parse(Json.toJson(codacyPatterns).toString).as[JsArray]
  }

  private def generateDescriptions(rules: Seq[Ruleset]): JsArray = {
    val codacyPatternsDescs = rules.map { rule =>
      Json.obj(
        "patternId" -> rule.patternId,
        "title" -> Json.toJsFieldJsValueWrapper(rule.title),
        "description" -> Json.toJsFieldJsValueWrapper(truncateText(rule.description, 495)),
        "timeToFix" -> 5
      )
    }

    Json.parse(Json.toJson(codacyPatternsDescs).toString).as[JsArray]
  }

  private def getRules(file: java.io.File): Seq[Ruleset] = {
    val outputXML: Elem = XML.loadFile(file)
    (outputXML \\ "errors" \\ "error").map { r =>
      Ruleset((r \ "@id").text, (r \ "@severity").text, (r \ "@msg").text, (r \ "@verbose").text)
    }
  }

  private def createPatternsAndDescriptionFile(rules: Seq[DocGenerator.Ruleset]): Unit = {
    val repoRoot: files.File = File(".")
    val docsRoot: files.File = File(repoRoot, "docs")
    val patternsFile: files.File = File(docsRoot, "patterns.json")
    val descriptionsRoot: files.File = File(docsRoot, "description")
    val descriptionsFile: files.File =
      File(descriptionsRoot, "description.json")

    val patterns: String = getPatterns(rules)
    val descriptions: String = getDescriptions(rules)

    patternsFile.write(s"${patterns}${System.lineSeparator}")
    descriptionsFile.write(s"${descriptions}${System.lineSeparator}")
  }

  private def misraPatternId(rule: String): String = s"misra-c2012-$rule"

  private def getMisraPatterns(): JsArray = {
    val misraPatterns = getMisraRules.map { rule =>
      Json.obj("patternId" -> misraPatternId(rule), "level" -> "Warning", "category" -> "ErrorProne")
    }

    Json.parse(Json.toJson(misraPatterns).toString).as[JsArray]
  }

  private def getMisraRules(): Seq[String] = {
    val misraRulesLines = File("addons/misra_rules.txt").lines
    misraRulesLines.filter(_.startsWith("Rule ")).map(_.stripPrefix("Rule ")).toSeq
  }

  private def getMisraDescription(): JsArray = {
    val misraDescriptions = getMisraRules.map { rule =>
      Json.obj(
        "patternId" -> misraPatternId(rule),
        "title" -> Json.toJsFieldJsValueWrapper(s"MISRA $rule rule"),
        "timeToFix" -> 5
      )
    }

    Json.parse(Json.toJson(misraDescriptions).toString).as[JsArray]
  }

  private def getAddonPatterns(): JsArray = {
    val patternsJson = File("addons/patterns.json").contentAsString
    (Json.parse(patternsJson) \ "patterns").as[JsArray] ++ getMisraPatterns()
  }

  private def getAddonDescription(): JsArray = {
    val descriptionJson = File("addons/description/description.json").contentAsString
    Json.parse(descriptionJson).as[JsArray] ++ getMisraDescription()
  }

  private def getPatterns(rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json.obj(
        "name" -> "cppcheck",
        "version" -> Versions.cppcheckVersion,
        "patterns" -> (Json
          .parse(Json.toJson(generatePatterns(rules)).toString)
          .as[JsArray] ++ getAddonPatterns())
      )
    )
  }

  private def getDescriptions(rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json
        .parse(Json.toJson(generateDescriptions(rules)).toString)
        .as[JsArray] ++ getAddonDescription()
    )
  }

  private def truncateText(description: String, maxCharacters: Int): String = {
    if (description.length > maxCharacters) {
      description
        .take(maxCharacters)
        .split("\\.")
        .dropRight(1)
        .mkString(".") + "."
    } else {
      description
    }
  }
}
