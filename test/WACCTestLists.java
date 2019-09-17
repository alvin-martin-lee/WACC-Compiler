public class WACCTestLists {
  public static String[] validTests = {
      // Advanced - not used in backend
      // "advanced/binarySortTree.wacc",
      // "advanced/hashTable.wacc",
      // "advanced/ticTacToe.wacc",

      // EXTENSION - Int Literals
      "intLiterals/exitBinary.wacc",
      "intLiterals/exitBinaryNegative.wacc",
      "intLiterals/exitDecimal.wacc",
      "intLiterals/exitDecimalNegative.wacc",
      "intLiterals/exitHexadecimal.wacc",
      "intLiterals/exitHexadecimalNegative.wacc",
      "intLiterals/exitOctal.wacc",
      "intLiterals/exitOctalNegative.wacc",
      "intLiterals/multipleIntLiterals.wacc",

      // EXTENSION - Bitwise
      "bitwise/bitwiseAnd.wacc",
      "bitwise/bitwiseNot.wacc",
      "bitwise/bitwiseOr.wacc",

      // EXTENSION - If
      "if/ifNoElseOddNumCounter.wacc",

      // EXTENSION - While
      "while/nestedWhileBreak.wacc",
      "while/whileProceed.wacc",

      // EXTENSION - Do While
      "doWhile/doWhileBasic.wacc",
      "doWhile/doWhileBreak.wacc",
      "doWhile/doWhileCount.wacc",
      "doWhile/doWhileProceed.wacc",

      // EXTENSION - For
      "for/forBasic.wacc",
      "for/forBreak.wacc",
      "for/forCount.wacc",
      "for/forProceed.wacc",

      // EXTENSION - Switch
      "switch/switchBasic.wacc",
      "switch/switchFallthrough.wacc",
      "switch/switchMultiCase.wacc",

      // EXTENSION - String Handling
      // "stringHandling/containsSubstring.wacc",
      "stringHandling/stringLiterConcat.wacc",

      // Array
      "array/array.wacc",
      "array/arrayBasic.wacc",
      "array/arrayEmpty.wacc",
      "array/arrayLength.wacc",
      "array/arrayLookup.wacc",
      "array/arrayNested.wacc",
      "array/arrayPrint.wacc",
      "array/arraySimple.wacc",
      "array/modifyString.wacc",
      "array/printRef.wacc",

      // Basic - Exit
      "basic/exit/exit-1.wacc",
      "basic/exit/exitBasic.wacc",
      "basic/exit/exitBasic2.wacc",
      "basic/exit/exitWrap.wacc",

      // Basic - Skip
      "basic/skip/comment.wacc",
      "basic/skip/commentInLine.wacc",
      "basic/skip/skip.wacc",

      // Expressions
      "expressions/andExpr.wacc",
      "expressions/boolCalc.wacc",
      "expressions/boolExpr1.wacc",
      "expressions/charComparisonExpr.wacc",
      "expressions/divExpr.wacc",
      "expressions/equalsExpr.wacc",
      "expressions/greaterEqExpr.wacc",
      "expressions/greaterExpr.wacc",
      "expressions/intCalc.wacc",
      "expressions/intExpr1.wacc",
      "expressions/lessCharExpr.wacc",
      "expressions/lessEqExpr.wacc",
      "expressions/lessExpr.wacc",
      "expressions/longExpr.wacc",
      "expressions/longExpr2.wacc",
      "expressions/longExpr3.wacc",
      "expressions/longSplitExpr.wacc",
      "expressions/longSplitExpr2.wacc",
      "expressions/minusExpr.wacc",
      "expressions/minusMinusExpr.wacc",
      "expressions/minusNoWhitespaceExpr.wacc",
      "expressions/minusPlusExpr.wacc",
      "expressions/modExpr.wacc",
      "expressions/multExpr.wacc",
      "expressions/multNoWhitespaceExpr.wacc",
      "expressions/negBothDiv.wacc",
      "expressions/negBothMod.wacc",
      "expressions/negDividendDiv.wacc",
      "expressions/negDividendMod.wacc",
      "expressions/negDivisorDiv.wacc",
      "expressions/negDivisorMod.wacc",
      "expressions/negExpr.wacc",
      "expressions/notequalsExpr.wacc",
      "expressions/notExpr.wacc",
      "expressions/ordAndchrExpr.wacc",
      "expressions/orExpr.wacc",
      "expressions/plusExpr.wacc",
      "expressions/plusMinusExpr.wacc",
      "expressions/plusNoWhitespaceExpr.wacc",
      // "expressions/plusPlusExpr.wacc",
      "expressions/sequentialCount.wacc",
      "expressions/stringEqualsExpr.wacc",

      // Function - Nested
      "function/nested_functions/fibonacciFullRec.wacc",
      "function/nested_functions/fibonacciRecursive.wacc",
      "function/nested_functions/fixedPointRealArithmetic.wacc",
      "function/nested_functions/functionConditionalReturn.wacc",
      "function/nested_functions/mutualRecursion.wacc",
      "function/nested_functions/printInputTriangle.wacc",
      "function/nested_functions/printTriangle.wacc",
      "function/nested_functions/simpleRecursion.wacc",

      // Function - Simple
      "function/simple_functions/asciiTable.wacc",
      "function/simple_functions/functionDeclaration.wacc",
      "function/simple_functions/functionManyArguments.wacc",
      "function/simple_functions/functionReturnPair.wacc",
      "function/simple_functions/functionSimple.wacc",
      "function/simple_functions/functionUpdateParameter.wacc",
      "function/simple_functions/incFunction.wacc",
      "function/simple_functions/negFunction.wacc",
      "function/simple_functions/sameArgName.wacc",
      "function/simple_functions/sameArgName2.wacc",

      // If
      "if/if1.wacc",
      "if/if2.wacc",
      "if/if3.wacc",
      "if/if4.wacc",
      "if/if5.wacc",
      "if/if6.wacc",
      "if/ifBasic.wacc",
      "if/ifFalse.wacc",
      "if/ifTrue.wacc",
      "if/whitespace.wacc",

      // IO
      "IO/IOLoop.wacc",
      "IO/IOSequence.wacc",

      // IO - Print
      "IO/print/hashInProgram.wacc",
      "IO/print/multipleStringsAssignment.wacc",
      "IO/print/print.wacc",
      "IO/print/printBool.wacc",
      "IO/print/printChar.wacc",
      "IO/print/printEscChar.wacc",
      "IO/print/printInt.wacc",
      "IO/print/println.wacc",
      "IO/print/stringAssignmentWithPrint.wacc",

      // IO - Read
      "IO/read/echoBigInt.wacc",
      "IO/read/echoBigNegInt.wacc",
      "IO/read/echoChar.wacc",
      "IO/read/echoInt.wacc",
      "IO/read/echoNegInt.wacc",
      "IO/read/echoPuncChar.wacc",
      "IO/read/read.wacc",

      // Pairs
      "pairs/checkRefPair.wacc",
      "pairs/createPair.wacc",
      "pairs/createPair02.wacc",
      "pairs/createPair03.wacc",
      "pairs/createRefPair.wacc",
      "pairs/free.wacc",
      "pairs/linkedList.wacc",
      "pairs/nestedPair.wacc",
      "pairs/null.wacc",
      "pairs/printNull.wacc",
      "pairs/printNullPair.wacc",
      "pairs/printPair.wacc",
      "pairs/printPairOfNulls.wacc",
      "pairs/readPair.wacc",
      "pairs/writeFst.wacc",
      "pairs/writeSnd.wacc",

      // Runtime Errors - Array out of Bounds
      "runtimeErr/arrayOutOfBounds/arrayNegBounds.wacc",
      "runtimeErr/arrayOutOfBounds/arrayOutOfBounds.wacc",
      "runtimeErr/arrayOutOfBounds/arrayOutOfBoundsWrite.wacc",

      // Runtime Errors - Divide by Zero
      "runtimeErr/divideByZero/divideByZero.wacc",
      "runtimeErr/divideByZero/divZero.wacc",
      "runtimeErr/divideByZero/modByZero.wacc",

      // Runtime Errors - Double Frees
      "runtimeErr/doubleFrees/doubleFree.wacc",
      "runtimeErr/doubleFrees/hiddenDoubleFree.wacc",

      // Runtime Errors - Integer Overflow
      "runtimeErr/integerOverflow/intJustOverflow.wacc",
      "runtimeErr/integerOverflow/intmultOverflow.wacc",
      "runtimeErr/integerOverflow/intnegateOverflow.wacc",
      "runtimeErr/integerOverflow/intnegateOverflow2.wacc",
      "runtimeErr/integerOverflow/intnegateOverflow3.wacc",
      "runtimeErr/integerOverflow/intnegateOverflow4.wacc",
      "runtimeErr/integerOverflow/intUnderflow.wacc",
      "runtimeErr/integerOverflow/intWayOverflow.wacc",

      // Runtime Errors - Null Dereference
      "runtimeErr/nullDereference/freeNull.wacc",
      "runtimeErr/nullDereference/readNull1.wacc",
      "runtimeErr/nullDereference/readNull2.wacc",
      "runtimeErr/nullDereference/setNull1.wacc",
      "runtimeErr/nullDereference/setNull2.wacc",
      "runtimeErr/nullDereference/useNull1.wacc",
      "runtimeErr/nullDereference/useNull2.wacc",

      // Scope
      "scope/ifNested1.wacc",
      "scope/ifNested2.wacc",
      "scope/indentationNotImportant.wacc",
      "scope/intsAndKeywords.wacc",
      "scope/printAllTypes.wacc",
      "scope/scope.wacc",
      "scope/scopeBasic.wacc",
      "scope/scopeRedefine.wacc",
      "scope/scopeSimpleRedefine.wacc",
      "scope/scopeVars.wacc",

      // Sequence
      "sequence/basicSeq.wacc",
      "sequence/basicSeq2.wacc",
      "sequence/boolAssignment.wacc",
      "sequence/charAssignment.wacc",
      "sequence/exitSimple.wacc",
      "sequence/intAssignment.wacc",
      "sequence/intLeadingZeros.wacc",
      "sequence/stringAssignment.wacc",

      // Variables
      "variables/_VarNames.wacc",
      "variables/boolDeclaration.wacc",
      "variables/boolDeclaration2.wacc",
      "variables/capCharDeclaration.wacc",
      "variables/charDeclaration.wacc",
      "variables/charDeclaration2.wacc",
      "variables/emptyStringDeclaration.wacc",
      "variables/intDeclaration.wacc",
      "variables/longVarNames.wacc",
      "variables/manyVariables.wacc",
      "variables/negIntDeclaration.wacc",
      "variables/puncCharDeclaration.wacc",
      "variables/stringDeclaration.wacc",
      "variables/zeroIntDeclaration.wacc",

      // While
      "while/fibonacciFullIt.wacc",
      "while/fibonacciIterative.wacc",
      "while/loopCharCondition.wacc",
      "while/loopIntCondition.wacc",
      "while/max.wacc",
      "while/min.wacc",
      "while/rmStyleAdd.wacc",
      "while/rmStyleAddIO.wacc",
      "while/whileBasic.wacc",
      "while/whileBoolFlip.wacc",
      "while/whileCount.wacc",
      "while/whileFalse.wacc"
  };

  public static String[] invalidSyntaxTests = {
      // Array
      "array/arrayExpr.wacc",

      // Basic
      "basic/badComment.wacc",
      "basic/badComment2.wacc",
      "basic/badEscape.wacc",
      "basic/beginNoend.wacc",
      "basic/bgnErr.wacc",
      "basic/multipleBegins.wacc",
      "basic/noBody.wacc",
      "basic/skpErr.wacc",
      "basic/unescapedChar.wacc",

      // Expressions
      "expressions/missingOperand1.wacc",
      "expressions/missingOperand2.wacc",
      // "expressions/printlnConcat.wacc",

      // Function
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

      // If
      "if/ifiErr.wacc",
      // "if/ifNoelse.wacc",
      "if/ifNofi.wacc",
      "if/ifNothen.wacc",

      // Pairs
      "pairs/badLookup01.wacc",
      "pairs/badLookup02.wacc",

      // Sequence
      "sequence/doubleSeq.wacc",
      "sequence/emptySeq.wacc",
      "sequence/endSeq.wacc",
      "sequence/extraSeq.wacc",
      "sequence/missingSeq.wacc",

      // Variables
      "variables/badintAssignments.wacc",
      "variables/bigIntAssignment.wacc",
      "variables/varNoName.wacc",

      // While
      "while/donoErr.wacc",
      "while/dooErr.wacc",
      "while/whileNodo.wacc",
      "while/whileNodone.wacc",
      "while/whilErr.wacc",
  };

  public static String[] invalidSemanticTests = {
      // Exit
      "exit/badCharExit.wacc",
      "exit/exitNonInt.wacc",
      "exit/globalReturn.wacc",

      // Expressions
      "expressions/boolOpTypeErr.wacc",
      "expressions/exprTypeErr.wacc",
      "expressions/intOpTypeErr.wacc",
      "expressions/lessPairExpr.wacc",
      "expressions/mixedOpTypeErr.wacc",
      "expressions/moreArrExpr.wacc",

      // Function
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

      // If
      "if/ifIntCondition.wacc",

      // IO
      "IO/readTypeErr.wacc",

      // Multiple
      "multiple/funcMess.wacc",
      "multiple/ifAndWhileErrs.wacc",
      "multiple/messyExpr.wacc",
      "multiple/multiCaseSensitivity.wacc",
      "multiple/multiTypeErrs.wacc",

      // Pairs
      "pairs/freeNonPair.wacc",

      // Print
      "print/printTypeErr01.wacc",

      // Read
      "read/readTypeErr01.wacc",

      // Scope
      "scope/badScopeRedefine.wacc",

      // Variables
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

      // While
      "while/falsErr.wacc",
      "while/truErr.wacc",
      "while/whileIntCondition.wacc",
  };
}
