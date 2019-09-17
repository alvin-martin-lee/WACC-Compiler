import os
import subprocess


def runValidTests(pathPrefix, typeName, successCode, tests):
  failedTests = []

  print("\n----------")
  print(typeName + " tests")
  print("----------")

  for test in tests:
    print('.', end='', flush=True)
    name = test["name"]
    code = subprocess.call(["./compile", pathPrefix + name], stdout=NULL, stderr=NULL)
    if code != successCode:
      failedTests.append(name)
    else:
      compiledName = name.split("/")[-1].replace(".wacc", ".s")
      subprocess.call(["arm-linux-gnueabi-gcc", "-o", "execTest", "-mcpu=arm1176jzf-s", "-mtune=arm1176jzf-s", compiledName], stdout=NULL, stderr=NULL)
      try:
        code = subprocess.call(["qemu-arm", "-L", "/usr/arm-linux-gnueabi/", "execTest"], stdout=NULL, stderr=NULL, timeout=60)
        print(code, end='', flush=True)
        if "code" in test:
          if code != test["code"]:
            failedTests.append(name)
        else:
          if code != successCode:
            failedTests.append(name)
      except subprocess.TimeoutExpired:
        failedTests.append(name)

  print("\nPassed " + str(len(tests) - len(failedTests)) + "/" + str(len(tests)) + " tests")

  if len(failedTests):
    print("Failing tests:")
    for test in failedTests:
      print(test)


def runTests(pathPrefix, typeName, successCode, tests):
  failedTests = []

  print("\n----------")
  print(typeName + " tests")
  print("----------")

  for test in tests:
    print('.', end='', flush=True)
    code = subprocess.call(["./compile", pathPrefix + test], stdout=NULL, stderr=NULL)
    if code != successCode:
      failedTests.append(test)

  print("\nPassed " + str(len(tests) - len(failedTests)) + "/" + str(len(tests)) + " tests")

  if len(failedTests):
    print("Failing tests:")
    for test in failedTests:
      print(test)



validTests = [
  # EXTENSION - Int Literals
  {"name": "intLiterals/exitBinary.wacc", "code": 5},
  {"name": "intLiterals/exitBinaryNegative.wacc", "code": 251},
  {"name": "intLiterals/exitDecimal.wacc", "code": 34},
  {"name": "intLiterals/exitDecimalNegative.wacc", "code": 222},
  {"name": "intLiterals/exitHexadecimal.wacc", "code": 17},
  {"name": "intLiterals/exitHexadecimalNegative.wacc", "code": 239},
  {"name": "intLiterals/exitOctal.wacc", "code": 15},
  {"name": "intLiterals/exitOctalNegative.wacc", "code": 241},
  {"name": "intLiterals/multipleIntLiterals.wacc", "code": 15},

  # EXTENSION - String Handling
  {"name": "stringHandling/containsSubstring.wacc"},
  {"name": "stringHandling/stringLiterConcat.wacc"},

  # EXTENSION - Bitwise
  {"name": "bitwise/bitwiseAnd.wacc"},
  {"name": "bitwise/bitwiseNot.wacc"},
  {"name": "bitwise/bitwiseOr.wacc"},

  # EXTENSION - If
  {"name": "if/ifNoElseOddNumCounter.wacc"},

  # EXTENSION - While
  {"name": "while/nestedWhileBreak.wacc"},
  {"name": "while/whileProceed.wacc"},

  # EXTENSION - Do While
  {"name": "doWhile/doWhileBasic.wacc"},
  {"name": "doWhile/doWhileBreak.wacc"},
  {"name": "doWhile/doWhileCount.wacc"},
  {"name": "doWhile/doWhileProceed.wacc"},

  # EXTENSION - For
  {"name": "for/forBasic.wacc"},
  {"name": "for/forBreak.wacc"},
  {"name": "for/forCount.wacc"},
  {"name": "for/forProceed.wacc"},

  # EXTENSION - Switch
  {"name": "switch/switchBasic.wacc"},
  {"name": "switch/switchFallthrough.wacc"},
  {"name": "switch/switchMultiCase.wacc"},

  # Array
  {"name": "array/array.wacc"},
  {"name": "array/arrayBasic.wacc"},
  {"name": "array/arrayEmpty.wacc"},
  {"name": "array/arrayLength.wacc"},
  {"name": "array/arrayLookup.wacc"},
  {"name": "array/arrayNested.wacc"},
  {"name": "array/arrayPrint.wacc"},
  {"name": "array/arraySimple.wacc"},
  {"name": "array/modifyString.wacc"},
  {"name": "array/printRef.wacc"},

  # Basic - Exit
  {"name": "basic/exit/exit-1.wacc", "code": -1},
  {"name": "basic/exit/exitBasic.wacc", "code": 7},
  {"name": "basic/exit/exitBasic2.wacc", "code": 42},
  {"name": "basic/exit/exitWrap.wacc"},

  # Basic - Skip
  {"name": "basic/skip/comment.wacc"},
  {"name": "basic/skip/commentInLine.wacc"},
  {"name": "basic/skip/skip.wacc"},

  # Expressions
  {"name": "expressions/andExpr.wacc"},
  {"name": "expressions/boolCalc.wacc"},
  {"name": "expressions/boolExpr1.wacc"},
  {"name": "expressions/charComparisonExpr.wacc"},
  {"name": "expressions/divExpr.wacc"},
  {"name": "expressions/equalsExpr.wacc"},
  {"name": "expressions/greaterEqExpr.wacc"},
  {"name": "expressions/greaterExpr.wacc"},
  {"name": "expressions/intCalc.wacc"},
  {"name": "expressions/intExpr1.wacc"},
  {"name": "expressions/lessCharExpr.wacc"},
  {"name": "expressions/lessEqExpr.wacc"},
  {"name": "expressions/lessExpr.wacc"},
  {"name": "expressions/longExpr.wacc"},
  {"name": "expressions/longExpr2.wacc"},
  {"name": "expressions/longExpr3.wacc"},
  {"name": "expressions/longSplitExpr.wacc"},
  {"name": "expressions/longSplitExpr2.wacc"},
  {"name": "expressions/minusExpr.wacc"},
  {"name": "expressions/minusMinusExpr.wacc"},
  {"name": "expressions/minusNoWhitespaceExpr.wacc"},
  {"name": "expressions/minusPlusExpr.wacc"},
  {"name": "expressions/modExpr.wacc"},
  {"name": "expressions/multExpr.wacc"},
  {"name": "expressions/multNoWhitespaceExpr.wacc"},
  {"name": "expressions/negBothDiv.wacc"},
  {"name": "expressions/negBothMod.wacc"},
  {"name": "expressions/negDividendDiv.wacc"},
  {"name": "expressions/negDividendMod.wacc"},
  {"name": "expressions/negDivisorDiv.wacc"},
  {"name": "expressions/negDivisorMod.wacc"},
  {"name": "expressions/negExpr.wacc"},
  {"name": "expressions/notequalsExpr.wacc"},
  {"name": "expressions/notExpr.wacc"},
  {"name": "expressions/ordAndchrExpr.wacc"},
  {"name": "expressions/orExpr.wacc"},
  {"name": "expressions/plusExpr.wacc"},
  {"name": "expressions/plusMinusExpr.wacc"},
  {"name": "expressions/plusNoWhitespaceExpr.wacc"},
  # {"name": "expressions/plusPlusExpr.wacc"},
  {"name": "expressions/sequentialCount.wacc"},
  {"name": "expressions/stringEqualsExpr.wacc"},

  # Function - Nested
  {"name": "function/nested_functions/fibonacciFullRec.wacc"},
  {"name": "function/nested_functions/fibonacciRecursive.wacc"},
  {"name": "function/nested_functions/fixedPointRealArithmetic.wacc"},
  {"name": "function/nested_functions/functionConditionalReturn.wacc"},
  {"name": "function/nested_functions/mutualRecursion.wacc"},
  {"name": "function/nested_functions/printInputTriangle.wacc"},
  {"name": "function/nested_functions/printTriangle.wacc"},
  {"name": "function/nested_functions/simpleRecursion.wacc"},

  # Function - Simple
  {"name": "function/simple_functions/asciiTable.wacc"},
  {"name": "function/simple_functions/functionDeclaration.wacc"},
  {"name": "function/simple_functions/functionManyArguments.wacc"},
  {"name": "function/simple_functions/functionReturnPair.wacc"},
  {"name": "function/simple_functions/functionSimple.wacc"},
  {"name": "function/simple_functions/functionUpdateParameter.wacc"},
  {"name": "function/simple_functions/incFunction.wacc"},
  {"name": "function/simple_functions/negFunction.wacc"},
  {"name": "function/simple_functions/sameArgName.wacc"},
  {"name": "function/simple_functions/sameArgName2.wacc"},

  # If
  {"name": "if/if1.wacc"},
  {"name": "if/if2.wacc"},
  {"name": "if/if3.wacc"},
  {"name": "if/if4.wacc"},
  {"name": "if/if5.wacc"},
  {"name": "if/if6.wacc"},
  {"name": "if/ifBasic.wacc"},
  {"name": "if/ifFalse.wacc"},
  {"name": "if/ifTrue.wacc"},
  {"name": "if/whitespace.wacc"},

  # IO
  {"name": "IO/IOLoop.wacc"},
  {"name": "IO/IOSequence.wacc"},

  # IO - Print
  {"name": "IO/print/hashInProgram.wacc"},
  {"name": "IO/print/multipleStringsAssignment.wacc"},
  {"name": "IO/print/print.wacc"},
  {"name": "IO/print/printBool.wacc"},
  {"name": "IO/print/printChar.wacc"},
  {"name": "IO/print/printEscChar.wacc"},
  {"name": "IO/print/printInt.wacc"},
  {"name": "IO/print/println.wacc"},
  {"name": "IO/print/stringAssignmentWithPrint.wacc"},

  # IO - Read
  {"name": "IO/read/echoBigInt.wacc"},
  {"name": "IO/read/echoBigNegInt.wacc"},
  {"name": "IO/read/echoChar.wacc"},
  {"name": "IO/read/echoInt.wacc"},
  {"name": "IO/read/echoNegInt.wacc"},
  {"name": "IO/read/echoPuncChar.wacc"},
  {"name": "IO/read/read.wacc"},

  # Pairs
  {"name": "pairs/checkRefPair.wacc"},
  {"name": "pairs/createPair.wacc"},
  {"name": "pairs/createPair02.wacc"},
  {"name": "pairs/createPair03.wacc"},
  {"name": "pairs/createRefPair.wacc"},
  {"name": "pairs/free.wacc"},
  {"name": "pairs/linkedList.wacc"},
  {"name": "pairs/nestedPair.wacc"},
  {"name": "pairs/null.wacc"},
  {"name": "pairs/printNull.wacc"},
  {"name": "pairs/printNullPair.wacc"},
  {"name": "pairs/printPair.wacc"},
  {"name": "pairs/printPairOfNulls.wacc"},
  {"name": "pairs/readPair.wacc"},
  {"name": "pairs/writeFst.wacc"},
  {"name": "pairs/writeSnd.wacc"},

  # Runtime Errors - Array out of Bounds
  {"name": "runtimeErr/arrayOutOfBounds/arrayNegBounds.wacc"},
  {"name": "runtimeErr/arrayOutOfBounds/arrayOutOfBounds.wacc"},
  {"name": "runtimeErr/arrayOutOfBounds/arrayOutOfBoundsWrite.wacc"},

  # Runtime Errors - Divide by Zero
  {"name": "runtimeErr/divideByZero/divideByZero.wacc"},
  {"name": "runtimeErr/divideByZero/divZero.wacc"},
  {"name": "runtimeErr/divideByZero/modByZero.wacc"},

  # Runtime Errors - Double Frees
  {"name": "runtimeErr/doubleFrees/doubleFree.wacc"},
  {"name": "runtimeErr/doubleFrees/hiddenDoubleFree.wacc"},

  # Runtime Errors - Integer Overflow
  {"name": "runtimeErr/integerOverflow/intJustOverflow.wacc"},
  {"name": "runtimeErr/integerOverflow/intmultOverflow.wacc"},
  {"name": "runtimeErr/integerOverflow/intnegateOverflow.wacc"},
  {"name": "runtimeErr/integerOverflow/intnegateOverflow2.wacc"},
  {"name": "runtimeErr/integerOverflow/intnegateOverflow3.wacc"},
  {"name": "runtimeErr/integerOverflow/intnegateOverflow4.wacc"},
  {"name": "runtimeErr/integerOverflow/intUnderflow.wacc"},
  {"name": "runtimeErr/integerOverflow/intWayOverflow.wacc"},

  # Runtime Errors - Null Dereference
  {"name": "runtimeErr/nullDereference/freeNull.wacc"},
  {"name": "runtimeErr/nullDereference/readNull1.wacc"},
  {"name": "runtimeErr/nullDereference/readNull2.wacc"},
  {"name": "runtimeErr/nullDereference/setNull1.wacc"},
  {"name": "runtimeErr/nullDereference/setNull2.wacc"},
  {"name": "runtimeErr/nullDereference/useNull1.wacc"},
  {"name": "runtimeErr/nullDereference/useNull2.wacc"},

  # Scope
  {"name": "scope/ifNested1.wacc"},
  {"name": "scope/ifNested2.wacc"},
  {"name": "scope/indentationNotImportant.wacc"},
  {"name": "scope/intsAndKeywords.wacc"},
  {"name": "scope/printAllTypes.wacc"},
  {"name": "scope/scope.wacc"},
  {"name": "scope/scopeBasic.wacc"},
  {"name": "scope/scopeRedefine.wacc"},
  {"name": "scope/scopeSimpleRedefine.wacc"},
  {"name": "scope/scopeVars.wacc"},

  # Sequence
  {"name": "sequence/basicSeq.wacc"},
  {"name": "sequence/basicSeq2.wacc"},
  {"name": "sequence/boolAssignment.wacc"},
  {"name": "sequence/charAssignment.wacc"},
  {"name": "sequence/exitSimple.wacc"},
  {"name": "sequence/intAssignment.wacc"},
  {"name": "sequence/intLeadingZeros.wacc"},
  {"name": "sequence/stringAssignment.wacc"},

  # Variables
  {"name": "variables/_VarNames.wacc"},
  {"name": "variables/boolDeclaration.wacc"},
  {"name": "variables/boolDeclaration2.wacc"},
  {"name": "variables/capCharDeclaration.wacc"},
  {"name": "variables/charDeclaration.wacc"},
  {"name": "variables/charDeclaration2.wacc"},
  {"name": "variables/emptyStringDeclaration.wacc"},
  {"name": "variables/intDeclaration.wacc"},
  {"name": "variables/longVarNames.wacc"},
  {"name": "variables/manyVariables.wacc"},
  {"name": "variables/negIntDeclaration.wacc"},
  {"name": "variables/puncCharDeclaration.wacc"},
  {"name": "variables/stringDeclaration.wacc"},
  {"name": "variables/zeroIntDeclaration.wacc"},

  # While
  {"name": "while/fibonacciFullIt.wacc"},
  {"name": "while/fibonacciIterative.wacc"},
  {"name": "while/loopCharCondition.wacc"},
  {"name": "while/loopIntCondition.wacc"},
  {"name": "while/max.wacc"},
  {"name": "while/min.wacc"},
  {"name": "while/rmStyleAdd.wacc"},
  {"name": "while/rmStyleAddIO.wacc"},
  {"name": "while/whileBasic.wacc"},
  {"name": "while/whileBoolFlip.wacc"},
  {"name": "while/whileCount.wacc"},
  {"name": "while/whileFalse.wacc"},
]

invalidSyntaxTests = [
  # Array
  "array/arrayExpr.wacc",

  # Basic
  "basic/badComment.wacc",
  "basic/badComment2.wacc",
  "basic/badEscape.wacc",
  "basic/beginNoend.wacc",
  "basic/bgnErr.wacc",
  "basic/multipleBegins.wacc",
  "basic/noBody.wacc",
  "basic/skpErr.wacc",
  "basic/unescapedChar.wacc",

  # Expressions
  "expressions/missingOperand1.wacc",
  "expressions/missingOperand2.wacc",
  # "expressions/printlnConcat.wacc",

  # Function
  "function/badlyNamed.wacc",
  "function/badlyPlaced.wacc",
  "function/funcExpr.wacc",
  "function/funcExpr2.wacc",
  "function/functionConditionalNoReturn.wacc",
  "function/functionJunkAfterReturn.wacc",
  "function/functionLateDefine.wacc",
  "function/functionMissingCall.wacc",
  "function/functionMissingParam.wacc",
  "function/functionMissingPType.wacc",
  "function/functionMissingType.wacc",
  "function/functionNoReturn.wacc",
  "function/functionScopeDef.wacc",
  "function/mutualRecursionNoReturn.wacc",
  "function/noBodyAfterFuncs.wacc",
  "function/thisIsNotC.wacc",

  # If
  "if/ifiErr.wacc",
  # "if/ifNoelse.wacc",
  "if/ifNofi.wacc",
  "if/ifNothen.wacc",

  # Pairs
  "pairs/badLookup01.wacc",
  "pairs/badLookup02.wacc",

  # Sequence
  "sequence/doubleSeq.wacc",
  "sequence/emptySeq.wacc",
  "sequence/endSeq.wacc",
  "sequence/extraSeq.wacc",
  "sequence/missingSeq.wacc",

  # Variables
  "variables/badintAssignments.wacc",
  "variables/bigIntAssignment.wacc",
  "variables/varNoName.wacc",

  # While
  "while/donoErr.wacc",
  "while/dooErr.wacc",
  "while/whileNodo.wacc",
  "while/whileNodone.wacc",
  "while/whilErr.wacc",
]

invalidSemanticTests = [
  # Exit
  "exit/badCharExit.wacc",
  "exit/exitNonInt.wacc",
  "exit/globalReturn.wacc",

  # Expressions
  "expressions/boolOpTypeErr.wacc",
  "expressions/exprTypeErr.wacc",
  "expressions/intOpTypeErr.wacc",
  "expressions/lessPairExpr.wacc",
  "expressions/mixedOpTypeErr.wacc",
  "expressions/moreArrExpr.wacc",

  # Function
  "function/functionAssign.wacc",
  "function/functionBadArgUse.wacc",
  "function/functionBadCall.wacc",
  "function/functionBadParam.wacc",
  "function/functionBadReturn.wacc",
  "function/functionOverArgs.wacc",
  "function/functionRedefine.wacc",
  "function/functionSwapArgs.wacc",
  "function/functionUnderArgs.wacc",
  "function/funcVarAccess.wacc",

  # If
  "if/ifIntCondition.wacc",

  # IO
  "IO/readTypeErr.wacc",

  # Multiple
  "multiple/funcMess.wacc",
  "multiple/ifAndWhileErrs.wacc",
  "multiple/messyExpr.wacc",
  "multiple/multiCaseSensitivity.wacc",
  "multiple/multiTypeErrs.wacc",

  # Pairs
  "pairs/freeNonPair.wacc",

  # Print
  "print/printTypeErr01.wacc",

  # Read
  "read/readTypeErr01.wacc",

  # Scope
  "scope/badScopeRedefine.wacc",

  # Variables
  "variables/basicTypeErr01.wacc",
  "variables/basicTypeErr02.wacc",
  "variables/basicTypeErr03.wacc",
  "variables/basicTypeErr04.wacc",
  "variables/basicTypeErr05.wacc",
  "variables/basicTypeErr06.wacc",
  "variables/basicTypeErr07.wacc",
  "variables/basicTypeErr08.wacc",
  "variables/basicTypeErr09.wacc",
  "variables/basicTypeErr10.wacc",
  "variables/basicTypeErr11.wacc",
  "variables/basicTypeErr12.wacc",
  "variables/caseMatters.wacc",
  "variables/doubleDeclare.wacc",
  "variables/undeclaredScopeVar.wacc",
  "variables/undeclaredVar.wacc",
  "variables/undeclaredVarAccess.wacc",

  # While
  "while/falsErr.wacc",
  "while/truErr.wacc",
  "while/whileIntCondition.wacc",
]

NULL = open(os.devnull, 'w')
VALID_CODE = 0
SYNTAX_CODE = 100
SEMANTIC_CODE = 200


print("\n#################")
print("# Running tests #")
print("#################")

runValidTests("test/valid/", "Valid", VALID_CODE, validTests)
runTests("test/invalid/syntaxErr/", "Invalid Syntax", SYNTAX_CODE, invalidSyntaxTests)
runTests("test/invalid/semanticErr/", "Invalid Semantic", SEMANTIC_CODE, invalidSemanticTests)
