package wacc

import antlr.WACCParser
import instr.*
import kotlin.math.log2

class WACCCodeGenerator(private var symbolTable: WACCSymbolTable) {

    private var labelCounter = 0
    private var afterLoopLabel = ""
    private var proceedJumpLabel = ""

    private val dataSegment = DataSegment(StringLiterals())
    private val runtimeErrors = RuntimeErrors(this)
    private val cLibraryCalls = CLibraryCalls(this)

    /**
     * Main Translation function
     */
    fun translate(node: WACCIdentifier): List<WACCInstruction> {
        return when (node) {
            is WACCProgram -> translateProgram(node)
            is WACCStatement -> translateStatement(node)
            is WACCExpression -> translateExpression(node)
            is WACCFunction -> translateFunction(node)
            is WACCPairLiter -> translatePairLiter()
            is WACCMathSTDFuncCall -> translateMathSTDFuncCall(node)
            else -> {
                throw Error("Unable to translate identifier of type $node")
            }
        }
    }

    private fun translatePairLiter(): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        instructs.add(LdrInstruction("0", WACCRegister.R4))
        return instructs
    }

    private fun translateStatement(stat: WACCStatement): List<WACCInstruction> {
        return when (stat.getStatType()) {
            WACCStatement.StatTypes.SKIP -> mutableListOf()
            WACCStatement.StatTypes.BREAK -> translateBreak()
            WACCStatement.StatTypes.PROCEED -> translateProceed()
            WACCStatement.StatTypes.ASSIGN -> translateAssign(stat)
            WACCStatement.StatTypes.READ -> translateRead(stat)
            WACCStatement.StatTypes.FREE -> translateFree(stat)
            WACCStatement.StatTypes.RETURN -> translateReturn(stat)
            WACCStatement.StatTypes.EXIT -> translateExit(stat)
            WACCStatement.StatTypes.PRINT -> translatePrint(stat)
            WACCStatement.StatTypes.PRINTLN -> translatePrintLn(stat)
            WACCStatement.StatTypes.IF -> translateIf(stat)
            WACCStatement.StatTypes.WHILE -> translateWhile(stat)
            WACCStatement.StatTypes.STAT_BLOCK -> translateStatBlock(stat.getChildStats()!![0])
            WACCStatement.StatTypes.STAT_MULT -> translateStatMulti(stat)
            WACCStatement.StatTypes.FOR -> translateFor(stat)
            WACCStatement.StatTypes.DO_WHILE -> translateWhile(stat, true)
            WACCStatement.StatTypes.SWITCH -> translateSwitch(stat)
            WACCStatement.StatTypes.STRLEN -> TODO()
            WACCStatement.StatTypes.STRLOWER -> TODO()
            WACCStatement.StatTypes.STRUPPER -> TODO()
        }
    }

    private fun translateFunction(function: WACCFunction): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        val name = createFunctionName(function)
        // f_functionname:
        instructs.add(FuncNameInstruction(name))

        // PUSH {lr}
        instructs.add(PushInstruction(WACCRegister.LR))

        // Function statements
        instructs.addAll(translateStatBlock(function.getRootStatement()!!))

        // POP {pc}
        instructs.add(PopInstruction(WACCRegister.PC))
        // POP {pc}
        instructs.add(PopInstruction(WACCRegister.PC))
        // .ltorg
        instructs.add(InlineLabelInstruction("ltorg"))

        return instructs
    }

    /**
     * Helper function for creating function names for name mangling.
     */
    private fun createFunctionName(function: WACCFunction): String {
        val name = function.getName()
        val returnType = function.getReturnType().getType().toString()
        val paramIterator = function.getParameters().iterator()
        var paramTypes = ""
        if (paramIterator.hasNext()) {
            for (param in paramIterator) {
                paramTypes = paramTypes.plus("_" + param.getIdent().getType().toString())
            }
        } else {
            paramTypes = "_NONE"
        }
        return "f_" + name + "_returns_" + returnType + "_params" + paramTypes
    }

    private fun translateExpression(expr: WACCExpression, reg1: WACCRegister = WACCRegister.R4): List<WACCInstruction> {
        return when (expr) {
            is WACCUnOperExpr -> translateUnOperExpr(expr, reg1)
            is WACCBinOperExpr -> translateBinOper(expr, reg1)
            else -> throw Error("Unreachable code")
        }
    }

    private fun translateUnOperExpr(expr: WACCUnOperExpr, reg1: WACCRegister): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val nextReg1 = reg1

        //LDR r4, =5
        instructs.addAll(genLoadExpr(expr.getLhs(), reg1))
        if (instructs.size == 0) {
            if (expr.getLhs() is WACCExpression) {
                instructs.addAll(translateExpression(expr.getLhs() as WACCExpression, nextReg1))
            } else {
                instructs.addAll(translate(expr.getLhs()))
            }
        }

        when (expr.getOper()) {
            WACCParser.NOT -> {
                // EOR r4, r4, #1
                instructs.add(EorInstruction(reg1, 1))
            }
            WACCParser.MINUS -> {
                // RSBS r4, r4, #0
                instructs.add(SubInstruction(reg1, 0, reverse = true, updateFlags = true))

                // BLVS p_throw_overflow_error
                instructs.add(BranchInstruction("p_throw_overflow_error", true, condition = WACCCondition.VS))

                runtimeErrors.addOverflowError()
            }
            WACCParser.LEN -> {
                // LDR r4, [r4]
                instructs.add(LdrInstruction(reg1, reg1))
            }
            WACCParser.ORD -> {
                // Intentionally left blank
            }
            WACCParser.CHR -> {
                // Intentionally left blank
            }
            WACCParser.BITWISE_NOT -> {
                // EOR r4, r4, #1
                instructs.add(EorInstruction(reg1, 0xFFFFFFFF))
            }
            else -> throw Error("Unreachable code")
        }

        return instructs
    }

    private fun translateBinOper(expr: WACCBinOperExpr, waccReg: WACCRegister = WACCRegister.R4): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        var needPopR11 = false

        var reg1 = waccReg

        if (reg1 >= WACCRegister.R10) {
            reg1 = WACCRegister.R10
            instructs.addAll(genLoadExpr(expr.getLhs(), reg1))
            instructs.add(PushInstruction(WACCRegister.R10))
            needPopR11 = true
        } else {
            instructs.addAll(genLoadExpr(expr.getLhs(), reg1))
        }

        val nextReg1 = reg1
        val reg2 = reg1.next()

        //LDR r4, =5
        if (instructs.size == 0) {
            if (expr.getLhs() is WACCExpression) {
                instructs.addAll(translateExpression(expr.getLhs() as WACCExpression, nextReg1))
            } else {
                instructs.addAll(translate(expr.getLhs()))
            }
        }

        //LDR r5, =5
        val rhsInstructs = mutableListOf<WACCInstruction>()

        if (reg2 >= WACCRegister.R10) {
            rhsInstructs.addAll(genLoadExpr(expr.getRhs(), WACCRegister.R10))
        } else {
            rhsInstructs.addAll(genLoadExpr(expr.getRhs(), reg2))
        }

        if (rhsInstructs.isNotEmpty()) {
            instructs.addAll(rhsInstructs)
        } else {
            if (expr.getRhs() is WACCExpression) {
                instructs.addAll(translateExpression(expr.getRhs() as WACCExpression, nextReg1.next()))
            } else {
                instructs.addAll(translate(expr.getRhs()))
            }
        }

        var operand1 = reg1
        var operand2 = reg2
        var destReg = reg1

        if (needPopR11) {
            instructs.add(PopInstruction(WACCRegister.R11))
            operand1 = reg2
            operand2 = destReg
            destReg = reg1
        }

        when (expr.getOper()) {
            WACCParser.MULTIPLY -> {
                // SMULL r4, r5, r4, r5
                instructs.add(LongMultiplyInstruction(reg1, reg2, reg1, reg2))
                // CMP r5, r4, ASR #31
                instructs.add(CmpInstruction(reg2, reg1, ArithmeticRightShift(31)))
                // BLNE p_throw_overflow_error
                instructs.add(BranchInstruction("p_throw_overflow_error", true, WACCCondition.NE))
                runtimeErrors.addOverflowError()
            }

            WACCParser.DIVIDE -> {
                // MOV r0, r4
                instructs.add(MovInstruction(reg1, WACCRegister.R0))
                // MOV r1, r5
                instructs.add(MovInstruction(reg2, WACCRegister.R1))
                // BL p_check_divide_by_zero
                instructs.add(BranchInstruction("p_check_divide_by_zero", true))
                runtimeErrors.addDivByZeroCheck()
                // BL __aeabi_idiv
                instructs.add(BranchInstruction("__aeabi_idiv", true))
                // MOV r4, r0
                instructs.add(MovInstruction(WACCRegister.R0, reg1))
            }

            WACCParser.MODULO -> {
                // MOV r0, r4
                instructs.add(MovInstruction(reg1, WACCRegister.R0))
                // MOV r1, r5
                instructs.add(MovInstruction(reg2, WACCRegister.R1))
                // BL p_check_divide_by_zero
                instructs.add(BranchInstruction("p_check_divide_by_zero", true))
                runtimeErrors.addDivByZeroCheck()
                // BL __aeabi_idivmod
                instructs.add(BranchInstruction("__aeabi_idivmod", true))
                // MOV r4, r1
                instructs.add(MovInstruction(WACCRegister.R1, reg1))
            }

            WACCParser.PLUS -> {
                // ADDS r4, r4, r5
                instructs.add(AddInstruction(operand1, operand2, destReg, updateFlags = true))
                // BLVS p_throw_overflow_error
                instructs.add(BranchInstruction("p_throw_overflow_error", true, WACCCondition.VS))
                runtimeErrors.addOverflowError()
            }

            WACCParser.MINUS -> {
                // SUBS r4, r4, r5
                instructs.add(SubInstruction(operand1, operand2, destReg, updateFlags = true))
                // BLVS p_throw_overflow_error
                instructs.add(BranchInstruction("p_throw_overflow_error", true, WACCCondition.VS))
                runtimeErrors.addOverflowError()
            }

            WACCParser.GREATER -> {
                // CMP r4, r5
                instructs.add(CmpInstruction(reg1, reg2))
                // MOVGT r4, #1
                instructs.add(MovInstruction(1, reg1, WACCCondition.GT))
                // MOVLE r4, #0
                instructs.add(MovInstruction(0, reg1, WACCCondition.LE))
            }

            WACCParser.GREATER_EQUAL -> {
                // CMP r4, r5
                instructs.add(CmpInstruction(reg1, reg2))
                // MOVGE r4, #1
                instructs.add(MovInstruction(1, reg1, WACCCondition.GE))
                // MOVLT r4, #0
                instructs.add(MovInstruction(0, reg1, WACCCondition.LT))
            }

            WACCParser.LESS -> {
                // CMP r4, r5
                instructs.add(CmpInstruction(reg1, reg2))
                // MOVLT r4, #1
                instructs.add(MovInstruction(1, reg1, WACCCondition.LT))
                // MOVGE r4, #0
                instructs.add(MovInstruction(0, reg1, WACCCondition.GE))
            }

            WACCParser.LESS_EQUAL -> {
                // CMP r4, r5
                instructs.add(CmpInstruction(reg1, reg2))
                // MOVLE r4, #1
                instructs.add(MovInstruction(1, reg1, WACCCondition.LE))
                // MOVGT r4, #0
                instructs.add(MovInstruction(0, reg1, WACCCondition.GT))
            }

            WACCParser.EQUAL -> {
                // CMP r4, r5
                instructs.add(CmpInstruction(reg1, reg2))
                // MOVEQ r4, #1
                instructs.add(MovInstruction(1, reg1, WACCCondition.EQ))
                // MOVNE r4, #0
                instructs.add(MovInstruction(0, reg1, WACCCondition.NE))
            }

            WACCParser.NOT_EQUAL -> {
                // CMP r4, r5
                instructs.add(CmpInstruction(reg1, reg2))
                // MOVNE r4, #1
                instructs.add(MovInstruction(1, reg1, WACCCondition.NE))
                // MOVEQ r4, #0
                instructs.add(MovInstruction(0, reg1, WACCCondition.EQ))
            }

            WACCParser.AND -> {
                // AND r4, r4, r5
                instructs.add(AndInstruction(reg1, reg2))
            }

            WACCParser.OR -> {
                // ORR r4, r4, r5
                instructs.add(OrrInstruction(reg1, reg2))
            }

            WACCParser.STRING_CONCAT -> {
                // Load LHS and RHS strings (string literal or ident), move into R0 and R1 for C library call
                // then save concatenated string into destination register destReg.
                cLibraryCalls.addCode(CLibraryCalls.CallType.CONCAT_STRINGS)
                instructs.addAll(listOf(
                        MovInstruction(fromReg = operand1, toReg = WACCRegister.R0),
                        MovInstruction(fromReg = operand2, toReg = WACCRegister.R1),
                        BranchInstruction(link = true, toLabel = CLibraryCalls.CallType.CONCAT_STRINGS.toString()),
                        MovInstruction(fromReg = WACCRegister.R0, toReg = destReg)
                ))
            }

            WACCParser.STRING_CONTAINS -> {
                cLibraryCalls.addCode(CLibraryCalls.CallType.STRING_CONTAINS)
                // call requires the string in R0 and substring in R1.
                instructs.addAll(listOf(
                        MovInstruction(fromReg = operand2, toReg = WACCRegister.R0),
                        MovInstruction(fromReg = operand1, toReg = WACCRegister.R1),
                        BranchInstruction(link = true, toLabel = CLibraryCalls.CallType.STRING_CONTAINS.toString()),
                        MovInstruction(fromReg = WACCRegister.R0, toReg = destReg)
                ))
            }

            WACCParser.BITWISE_AND -> {
                // AND r4, r4, r5
                instructs.add(AndInstruction(reg1, reg2))
            }

            WACCParser.BITWISE_OR -> {
                // ORR r4, r4, r5
                instructs.add(OrrInstruction(reg1, reg2))
            }

            else -> throw Error("Unreachable code")
        }

        return instructs
    }

    private fun translateStatBlock(stat: WACCStatement): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val parentSymTab = symbolTable

        symbolTable = stat.getSymbolTable()
        symbolTable.initializeLocalVariableStackOffsets()

        // Enter a scope
        val stackSpaceSize = stat.getSymbolTable().scopeStackSize()
        if (stackSpaceSize > 0) {
            instructs.add(SubInstruction(WACCRegister.SP, stackSpaceSize))
        }

        // Visit main program and add to instruction list
        instructs.addAll(translate(stat))

        // Exit a scope
        if (stackSpaceSize > 0) {
            instructs.add(AddInstruction(WACCRegister.SP, stackSpaceSize))
        }

        symbolTable = parentSymTab

        return instructs
    }

    private fun translateProgram(program: WACCProgram): List<WACCInstruction> {
        // Translate function definitions
        val functionInstructions = mutableListOf<WACCInstruction>()
        for (function in program.getFuncs()) {
            functionInstructions.addAll(translate(function))
        }

        val mainInstructions = mutableListOf<WACCInstruction>()
        mainInstructions.add(FuncNameInstruction("main"))
        // AI: PUSH {lr}
        mainInstructions.add(PushInstruction(WACCRegister.LR))

        // Visit main program and add to instruction list
        mainInstructions.addAll(translateStatBlock(program.getStat()))

        // AI: LDR r0, =0
        mainInstructions.add(LdrInstruction("0", WACCRegister.R0))

        // AI: POP {pc}
        mainInstructions.add(PopInstruction(WACCRegister.PC))

        // .ltorg
        mainInstructions.add(InlineLabelInstruction("ltorg"))

        // Add data section (strings)
        val dataSegmentInstructs: List<WACCInstruction> = dataSegment.translate()

        // Put everything together
        val result = dataSegmentInstructs +
                listOf(LabelInstruction("text")) +
                listOf(LabelInstruction("global main")) +
                functionInstructions +
                mainInstructions +
                runtimeErrors.translate() +
                cLibraryCalls.translate()
        return result
    }

    fun translateAssign(stat: WACCStatement): List<WACCInstruction> {
        assert(stat.getStatType() == WACCStatement.StatTypes.ASSIGN)
        val instructs = mutableListOf<WACCInstruction>()
        var offset = 0
        val lhs = stat.getLhs()
        when (lhs) {
            is WACCIdent -> {
                offset = symbolTable.calculateStackOffset(lhs.toString(), lhs.getType())
            }
            is WACCArrayElem -> {
                val rhsValueReg = WACCRegister.R4   // value of RHS here
                val arrayPtrReg = WACCRegister.R5   // pointer to a location in the array
                val arrayIndexReg = WACCRegister.R6 // array index

                val rhs = stat.getRhs()
                // Array element assignment requires RHS to be evaulated first (see comment above return statement)
                when (rhs) {
                    is WACCIntLiter,
                    is WACCBoolLiter,
                    is WACCCharLiter,
                    is WACCStringLiter -> {
                        instructs.addAll(genLoadExpr(rhs, rhsValueReg))
                    }
                    else -> {
                        val temp = genLoadExpr(rhs!!, rhsValueReg)
                        instructs.addAll(temp)
                    }
                }

                // Now handle LHS, assuming R4 is reserved for the RHS value.
                instructs.addAll(genStoreArrayElem(lhs, rhs, arrayPtrReg, arrayIndexReg, rhsValueReg))

                // IMPORTANT: here is an early return here because for array element assignment the rhs
                // has to be evaluated first into the register R4. There is some overlap between here
                // and the rest of the code, but moving when (rhs) {...} might mess up other cases.
                return instructs
            }
        }

        val rhs = stat.getRhs()!!

        when (rhs) {
            is WACCIntLiter,
            is WACCBoolLiter,
            is WACCCharLiter,
            is WACCStringLiter -> {
                instructs.addAll(translateAssignBasic(rhs, offset, WACCRegister.R4))
            }
            is WACCArrayLit -> {
                // LDR R0, =array_size
                val numElements = rhs.getArrayElems().size
                val elemSize = rhs.getElemType().size
                val mallocSize = WACCType.INT.size + numElements * elemSize

                // registers to use
                val mallocReg = WACCRegister.R0
                val arrayPtrReg = WACCRegister.R4
                val tempReg = WACCRegister.R5

                // Allocate heap memory and get array address by calling malloc
                instructs.add(LdrInstruction(toReg = mallocReg, fromLabel = mallocSize.toString()))
                instructs.add(BranchInstruction(CLibraryCalls.CLibFunctions.MALLOC.toString(), link = true))
                instructs.add(MovInstruction(toReg = arrayPtrReg, fromReg = mallocReg))

                val arrayElems = rhs.getArrayElems()
                for (i in 0 until numElements) {
                    val arrayOffset = (i + 1) * elemSize
                    val elemInstructs = mutableListOf<WACCInstruction>()
                    when (arrayElems[i]) {
                        is WACCIntLiter,
                        is WACCStringLiter,
                        is WACCBoolLiter,
                        is WACCCharLiter -> elemInstructs.addAll(
                                translateAssignBasic(arrayElems[i], toReg = tempReg, addrAtReg = arrayPtrReg, offset = arrayOffset))
                        is WACCIdent -> {
                            elemInstructs.addAll(genLoadExpr(arrayElems[i], tempReg))
                            elemInstructs.addAll(genStoreExpr(lhs!!, fromReg = tempReg, addrAtReg = arrayPtrReg, offset = arrayOffset))
                        }
                        else -> {
                            throw Error("Unable to translate array elem")
                        }
                    }
                    instructs.addAll(elemInstructs)
                }
                // Save array size LDR r5, =size; STR r5, [r4]
                instructs.addAll(listOf(
                        LdrInstruction(toReg = tempReg, fromLabel = numElements.toString()),
                        StrInstruction(fromReg = tempReg, toAddrAtReg = arrayPtrReg)
                ))
                // Store heap address returned by malloc at top of stack
                val arrayOffset = symbolTable.calculateStackOffset(lhs.toString())
                instructs.add(StrInstruction(fromReg = arrayPtrReg, toAddrAtReg = WACCRegister.SP, offset = arrayOffset))

                // rhs = WACCIdentifier(rhs.getElemType())
                // instructs.addAll(translateAssignBasic(rhs, offset, WACCRegister.R5))
            }
            is WACCPairLiter -> {
                instructs.addAll(translatePairLiter())
                instructs.addAll(genStoreExpr(stat.getLhs()!!, WACCRegister.R4, offset))

            }
            is WACCPair -> {
                val fstElem = rhs.getValue1()
                val sndElem = rhs.getValue2()
                // BL malloc
                instructs.add(BranchInstruction(CLibraryCalls.CLibFunctions.MALLOC.toString(), link = true))
                // MOV r4, r0
                instructs.add(MovInstruction(WACCRegister.R0, WACCRegister.R4))

                instructs.addAll(translateAssignPairHelper(fstElem, offset))

                // BL malloc
                instructs.add(BranchInstruction(CLibraryCalls.CLibFunctions.MALLOC.toString(), link = true))
                // STR(B) r5, [r0]
                instructs.add(StrInstruction(WACCRegister.R5, WACCRegister.R0, singleByte = (fstElem == WACCType.BOOL || fstElem == WACCType.CHAR)))
                // STR r0, [r4]
                instructs.add(StrInstruction(WACCRegister.R0, WACCRegister.R4))

                instructs.addAll(translateAssignPairHelper(sndElem, offset))

                // BL malloc
                instructs.add(BranchInstruction(CLibraryCalls.CLibFunctions.MALLOC.toString(), link = true))
                // STR(B) r5, [r0]
                instructs.add(StrInstruction(WACCRegister.R5, WACCRegister.R0, singleByte = (sndElem == WACCType.BOOL || sndElem == WACCType.CHAR)))
                // STR r0, [r4, #4]
                instructs.add(StrInstruction(WACCRegister.R0, WACCRegister.R4, offset = offset))
                // STR r4, [sp]
                instructs.add(StrInstruction(WACCRegister.R4, WACCRegister.SP))
            }
            is WACCExpression -> {
                instructs.addAll(translateExpression(rhs))
                instructs.addAll(genStoreExpr(rhs, WACCRegister.R4, offset))
            }
            is WACCIdent -> {
                instructs.addAll(genLoadExpr(stat.getRhs()!!, WACCRegister.R4))
                val stackOffset = symbolTable.calculateStackOffset(stat.getLhs()!!.toString())
                instructs.addAll(genStoreExpr(stat.getLhs()!!, WACCRegister.R4, stackOffset))
            }
            is WACCFunctionCall -> {

                // Load Parameters
                val name = rhs.getFuncName()
                val returnType = rhs.getType().toString()
                val arguments = rhs.getArgs().getArgTypes()
                val argumentIterator = arguments.iterator()
                var argumentTypes = ""
                if (argumentIterator.hasNext()) {
                    for (argument in argumentIterator) {
                        argumentTypes = argumentTypes.plus("_${argument.getType()}")
                    }
                } else {
                    argumentTypes = "_NONE"
                }
                // Full function name including return and param types
                val fullFuncName = name + "_returns_" + returnType + "_params" + argumentTypes
                var totalOffset = 0

                for (arg in arguments) {
                    instructs.addAll(genLoadExpr(arg, WACCRegister.R4))
                    val offset = arg.getType().size
                    totalOffset += offset
                    if (arg is WACCExpression) {
                        instructs.addAll(translate(arg))
                    }
                    instructs.addAll(genStoreExpr(arg, WACCRegister.R4, -offset, true))
                }

                instructs.add(BranchInstruction("f_$fullFuncName", link = true))

                if (totalOffset > 0) {
                    instructs.add(AddInstruction(WACCRegister.SP, totalOffset))
                }

                instructs.add(MovInstruction(WACCRegister.R0, WACCRegister.R4))
                instructs.addAll(genStoreExpr(rhs, WACCRegister.R4, offset))
            }
        }
        return instructs
    }

    /**
     * Helper function for WACCPair case
     */

    private fun translateAssignPairHelper(elem: WACCIdentifier, offset: Int): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        when (elem) {
            is WACCIntLiter -> {
                val value: Long = elem.getValue()
                // LDR r5, =fstVal
                instructs.add(LdrInstruction(value.toString(), WACCRegister.R5))
                // LDR r0, =4
                instructs.add(LdrInstruction(WACCType.INT.size.toString(), WACCRegister.R0))
            }
            is WACCStringLiter -> {
                // add to .data
                val label = dataSegment.addString(elem.toString())
                // LDR r5, =fstVal
                instructs.add(LdrInstruction(label, WACCRegister.R5))
                // LDR r0, =4
                instructs.add(LdrInstruction(WACCType.ARRAY.size.toString(), WACCRegister.R0))
            }
            is WACCCharLiter -> {
                val value: Char = elem.getValue()
                // MOV r5, #fstVal
                instructs.add(MovInstruction(value, WACCRegister.R5))
                // LDR r0, =1
                instructs.add(LdrInstruction(WACCType.CHAR.size.toString(), WACCRegister.R0))
            }
            is WACCBoolLiter -> {
                // MOV r5, #fstVal
                instructs.add(MovInstruction(elem.getValue().toInt(), WACCRegister.R5))
                // LDR r0, =1
                instructs.add(LdrInstruction(WACCType.BOOL.size.toString(), WACCRegister.R0))
            }
            is WACCArray -> throw NotImplementedError()
            is WACCPair -> {
                // LDR r5, [sp, #offset]
                instructs.add(LdrInstruction(WACCRegister.SP, WACCRegister.R5, offset = offset))
                // LDR r0, =4
                instructs.add(LdrInstruction(WACCType.PAIR.size.toString(), WACCRegister.R0))
            }
        }
        return instructs
    }

    /**
     * Helper function for ASSIGN statements
     */

    private fun translateAssignBasic(rhs: WACCIdentifier, offset: Int, toReg: WACCRegister, addrAtReg: WACCRegister = WACCRegister.SP): MutableList<WACCInstruction> {
        // Examples in comments: toReg = r4, addrAtReg = SP
        val instructs = mutableListOf<WACCInstruction>()
        when (rhs) {
            is WACCIntLiter -> {
                val value: Long = rhs.getValue()
                // LDR r4, =val
                instructs.add(LdrInstruction(value.toString(), toReg))
                // STR r4, [sp, #offset]
                instructs.add(StrInstruction(toReg, addrAtReg, offset = offset))
            }
            is WACCStringLiter -> {
                val value: String = rhs.getValue()
                // add string to .data
                val label = dataSegment.addString(value)
                // LDR r4, =val
                instructs.add(LdrInstruction(label, toReg))
                // STR r4, [sp, #offset]
                instructs.add(StrInstruction(toReg, addrAtReg, offset = offset))
            }
            is WACCBoolLiter -> {
                // MOV r4, #val
                instructs.add(MovInstruction(rhs.getValue().toInt(), toReg))
                // STRB r4, [sp, #offset]
                instructs.add(StrInstruction(toReg, addrAtReg, offset = offset, singleByte = true))
            }
            is WACCCharLiter -> {
                val value: Char = rhs.getValue()
                // MOV r4, #val
                instructs.add(MovInstruction(value, toReg))
                // STRB r4, [sp, #offset]
                instructs.add(StrInstruction(toReg, addrAtReg, offset = offset, singleByte = true))
            }
        }
        return instructs
    }

    fun translateReturn(stat: WACCStatement): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val exprInstructs = genLoadExpr(stat.getExpr()!!, WACCRegister.R4)
        if (exprInstructs.isEmpty()) {
            instructs.addAll(translate(stat.getExpr() as WACCIdentifier))
        } else {
            instructs.addAll(exprInstructs)
        }

        instructs.add(MovInstruction(WACCRegister.R4, WACCRegister.R0))
        return instructs
    }

    fun translateExit(stat: WACCStatement): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        // Get the exit code
        val exitCodeExpr = stat.getExpr()!!
        val exitCode: Long

        if (exitCodeExpr is WACCIdent) {
            val offset = symbolTable.calculateStackOffset(exitCodeExpr.toString())
            // LDR r4, =exitCode
            instructs.add(LdrInstruction(WACCRegister.SP, WACCRegister.R4, offset))
        }

        if (exitCodeExpr is WACCIntLiter) {
            exitCode = exitCodeExpr.getValue()
            // LDR r4, =exitCode
            instructs.add(LdrInstruction(exitCode.toString(), WACCRegister.R4))
        }

        if (exitCodeExpr is WACCExpression) {
            instructs.addAll(translate(stat.getExpr()!!))
        }

        // MOV r0, r4
        instructs.add(MovInstruction(WACCRegister.R4, WACCRegister.R0))

        // BL exit
        instructs.add(BranchInstruction("exit", true))

        return instructs
    }

    /**
     * Translate read statement "read x".
     */
    fun translateRead(stat: WACCStatement): List<WACCInstruction> {
        assert(stat.getStatType() == WACCStatement.StatTypes.READ)
        assert(stat.getLhs() != null)
        val assignLhs = stat.getLhs()!!

        val instructs: MutableList<WACCInstruction> = mutableListOf()

        // Assumes it saves the memory address of the variable/pairelem/arrayelem to register R4
        // val assignLhsInstructs: List<WACCInstruction> = translateAssignLhs(assignLhs)
        // instructs.addAll(assignLhsInstructs)

        // This handles reading to a variable only
        if (assignLhs is WACCIdent) {
            val offset = symbolTable.calculateStackOffset(assignLhs.toString())
            instructs.add(AddInstruction(destReg = WACCRegister.R4,
                    operand1Reg = WACCRegister.SP,
                    operand2Imm = offset))
        }
        // Assumes the value of expression is stored in register R4
        // Assumes R0 is the first argument
        val resultReg: WACCRegister = WACCRegister.R4
        val paramReg: WACCRegister = WACCRegister.R0
        instructs.add(MovInstruction(resultReg, paramReg))
        val cLibFunctionCall = when (assignLhs.getType()) {
            WACCType.INT -> CLibraryCalls.CallType.READ_INT
            WACCType.CHAR -> CLibraryCalls.CallType.READ_CHAR
            else -> throw Exception()
        }
        cLibraryCalls.addCode(cLibFunctionCall)
        instructs.add(BranchInstruction(cLibFunctionCall.toString(), true))
        return instructs
    }

    /**
     * Translate print statements "print x"
     */
    fun translatePrint(stat: WACCStatement): List<WACCInstruction> {
        assert(stat.getStatType() == WACCStatement.StatTypes.PRINT)
        assert(stat.getExpr() != null)
        val instructs: List<WACCInstruction> = translatePrintHelper(stat)
        return instructs
    }

    /**
     * Translate print statements "println x"
     */
    fun translatePrintLn(stat: WACCStatement): List<WACCInstruction> {
        assert(stat.getStatType() == WACCStatement.StatTypes.PRINTLN)
        assert(stat.getExpr() != null)
        val instructs: MutableList<WACCInstruction> = mutableListOf()
        instructs.addAll(translatePrintHelper(stat))
        instructs.add(BranchInstruction(CLibraryCalls.CallType.PRINT_LN.toString(), true))
        cLibraryCalls.addCode(CLibraryCalls.CallType.PRINT_LN)
        return instructs
    }

    /**
     * Helper function for PRINT and PRINTLN statements.
     *
     * The generated code for PRINTLN is exactly the same as PRINT
     * except it calls "p_print_ln" afterward to print a newline character.
     * This function generates the part of the code that is the same for both,
     * i.e. the non-newline part.
     */
    private fun translatePrintHelper(stat: WACCStatement): List<WACCInstruction> {
        assert(stat.getStatType() == WACCStatement.StatTypes.PRINT ||
                stat.getStatType() == WACCStatement.StatTypes.PRINTLN)
        assert(stat.getExpr() != null)

        val instructs: MutableList<WACCInstruction> = mutableListOf()
        // val expr: WACCExpression = stat.getExpr()!! as WACCExpression
        // val exprInstructs: List<WACCInstruction> = translateExpression(expr)
        // instructs.addAll(exprInstructs)

        val expr = stat.getExpr()
        if (expr is WACCStringLiter) {
            val label = dataSegment.addString(expr.getValue())
            instructs.add(LdrInstruction(fromLabel = label, toReg = WACCRegister.R4))
        } else if (expr is WACCArrayElem) {
            instructs.addAll(genLoadArrayElem(expr, WACCRegister.R4, WACCRegister.R4, WACCRegister.R6))
        } else if (expr is WACCIdent && expr.getType() == WACCType.ARRAY) {
            // For array argument, print reference (memory location)
            val offset = symbolTable.calculateStackOffset(expr.toString())
            instructs.add(LdrInstruction(fromAddrAtReg = WACCRegister.SP, offset = offset, toReg = WACCRegister.R4))
        } else {
            val loadExprInstr = genLoadExpr(expr!!, WACCRegister.R4)

            if (loadExprInstr.isNotEmpty()) {
                instructs.addAll(loadExprInstr)
            } else {
                instructs.addAll(translate(expr))
            }
        }

        // Assumes result of expression is stored in register R4
        // Assumes R0 is the first argument
        instructs.add(MovInstruction(fromReg = WACCRegister.R4, toReg = WACCRegister.R0))

        // Call C library function depending on type
        val type = expr.getType()
        val entry = symbolTable.lookupAll(expr.toString())

        val functionLabel: String?
        if (type == WACCType.CHAR) {
            // calls putchar directly, does not generate a custom function
            functionLabel = CLibraryCalls.CLibFunctions.PUTCHAR.toString()
        } else if (expr is WACCIdent && type == WACCType.ARRAY && entry is WACCArrayLit && entry.getElemType() != WACCType.CHAR) {
            // print reference (address) of array only if it isn't a string (char array)
            // otherwise print out the string instead of the address in next else block
            val callType = CLibraryCalls.CallType.PRINT_REFERENCE
            functionLabel = callType.toString()
            cLibraryCalls.addCode(callType)
        } else if (type == WACCType.PAIR) {
            val callType = CLibraryCalls.CallType.PRINT_REFERENCE
            functionLabel = callType.toString()
            cLibraryCalls.addCode(callType)
        } else if (expr is WACCArrayElem) {
            val temp = symbolTable.lookupAll(expr.getArrayIdent().toString())
            require(temp is WACCArray)
            val type = temp.getArrayCellBaseType()
            if (type == WACCType.CHAR) {
                functionLabel = CLibraryCalls.CLibFunctions.PUTCHAR.toString()
            } else {
                val callType = when (type) {
                    WACCType.INT -> CLibraryCalls.CallType.PRINT_INT
                    WACCType.BOOL -> CLibraryCalls.CallType.PRINT_BOOL
                    else -> throw NotImplementedError()
                }
                cLibraryCalls.addCode(callType)
                functionLabel = callType.toString()
            }
        } else {
            // rest need to generate custom function-calling code (prefixed with "p_")
            val callType = when (type) {
                WACCType.INT -> CLibraryCalls.CallType.PRINT_INT
                WACCType.ARRAY -> CLibraryCalls.CallType.PRINT_STRING
                WACCType.BOOL -> CLibraryCalls.CallType.PRINT_BOOL
                else -> {
                    throw Exception("PRINT type error: semantic analyser should have handled this")
                }
            }
            cLibraryCalls.addCode(callType)
            functionLabel = callType.toString()
        }
        instructs.add(BranchInstruction(functionLabel, link = true))
        return instructs
    }

    fun translateStatMulti(stat: WACCStatement): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val childStats = stat.getChildStats()!!

        instructs.addAll(translateStatement(childStats[0]))
        instructs.addAll(translateStatement(childStats[1]))

        return instructs
    }

    fun translateBreak(): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        instructs.add(BranchInstruction(afterLoopLabel))

        return instructs
    }

    fun translateProceed(): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        instructs.add(BranchInstruction(proceedJumpLabel))

        return instructs
    }

    fun translateWhile(stat: WACCStatement, isDoWhile: Boolean = false): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val predicateLabel = getNewLabel()
        val bodyLabel = getNewLabel()

        // Sets label for Break statements
        val prevAfterLoopLabel = afterLoopLabel
        if (stat.getContainsBreak()) {
            afterLoopLabel = getNewLabel()
        }

        // Sets label for Proceeds functions
        val prevPredLabel = proceedJumpLabel
        proceedJumpLabel = predicateLabel

        if (!isDoWhile) {
            // Branch to predicate (Instruct: B label1)
            instructs.add(BranchInstruction(predicateLabel))
        }

        // Get body instructions
        instructs.add(FuncNameInstruction(bodyLabel))
        instructs.addAll(translateStatement(stat.getChildStats()!![0]))

        // Get predicate instructions
        instructs.add(FuncNameInstruction(predicateLabel))
        if (stat.getExpr()!! is WACCExpression) {
            instructs.addAll(translate(stat.getExpr()!!))
        } else {
            instructs.addAll(genLoadExpr(stat.getExpr()!!, WACCRegister.R4))
        }
        // CMP r4, #1
        instructs.add(CmpInstruction(WACCRegister.R4, 1))

        // BEQ bodyLabel
        instructs.add(BranchInstruction(bodyLabel, condition = WACCCondition.EQ))

        if (stat.getContainsBreak()) {
            instructs.add(FuncNameInstruction(afterLoopLabel))
            afterLoopLabel = prevAfterLoopLabel
        }

        proceedJumpLabel = prevPredLabel

        return instructs
    }

    fun translateMathSTDFuncCall(stat: WACCIdentifier): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val mathStat = stat as WACCMathSTDFuncCall
        // Load Parameters

        TODO()

        return instructs
    }

    fun translateFor(stat: WACCStatement): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        // Initialises for loop counter
        instructs.addAll(translate(stat.getLhs()!!))

        val condLabel = getNewLabel()
        val bodyLabel = getNewLabel()
        val incLabel = getNewLabel()

        val prevAfterLoopLabel = afterLoopLabel
        if (stat.getContainsBreak()) {
            afterLoopLabel = getNewLabel()
        }

        // Sets label for Proceeds functions
        val prevPredLabel = proceedJumpLabel
        proceedJumpLabel = incLabel

        // Branch to condition (Instruct: B label1)
        instructs.add(BranchInstruction(condLabel))

        // Get body instructions
        instructs.add(FuncNameInstruction(bodyLabel))
        instructs.addAll(translateStatement(stat.getChildStats()!![0]))

        // Increments counter
        instructs.add(FuncNameInstruction(incLabel))
        instructs.addAll(translate(stat.getRhs()!!))

        // Get condition instructions
        instructs.add(FuncNameInstruction(condLabel))
        if (stat.getExpr()!! is WACCExpression) {
            instructs.addAll(translate(stat.getExpr()!!))
        } else {
            instructs.addAll(genLoadExpr(stat.getExpr()!!, WACCRegister.R4))
        }
        // CMP r4, #1
        instructs.add(CmpInstruction(WACCRegister.R4, 1))

        // BEQ bodyLabel
        instructs.add(BranchInstruction(bodyLabel, condition = WACCCondition.EQ))

        if (stat.getContainsBreak()) {
            instructs.add(FuncNameInstruction(afterLoopLabel))
            afterLoopLabel = prevAfterLoopLabel
        }

        proceedJumpLabel = prevPredLabel

        return instructs
    }

    fun translateIf(stat: WACCStatement): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        val childStats = stat.getChildStats()!!
        val hasElseBranch = childStats.size > 1

        val elseBodyLabel = getNewLabel()
        val afterIfStatLabel = getNewLabel()

        // Add predicate instructions
        if (stat.getExpr()!! is WACCExpression) {
            instructs.addAll(translate(stat.getExpr()!!))
        } else {
            instructs.addAll(genLoadExpr(stat.getExpr()!!, WACCRegister.R4))
        }

        // CMP r4, #0
        instructs.add(CmpInstruction(WACCRegister.R4, 0))

        // BEQ elseBodyLabel
        instructs.add(BranchInstruction(elseBodyLabel, condition = WACCCondition.EQ))

        // If predicate is true code body
        instructs.addAll(translateStatBlock(childStats[0]))

        // B afterIfStatLabel
        instructs.add(BranchInstruction(afterIfStatLabel))

        // L0:
        instructs.add(FuncNameInstruction(elseBodyLabel))

        if (hasElseBranch) {
            // Else body
            instructs.addAll(translateStatBlock(childStats[1]))
        }

        // L1:
        instructs.add(FuncNameInstruction(afterIfStatLabel))

        return instructs
    }

    fun translateSwitch(stat: WACCStatement): MutableList<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        val caseLabels = mutableListOf<String>()

        val reg1 = WACCRegister.R4
        val reg2 = WACCRegister.R5

        val switchStat = stat as WACCSwitch
        val ident = switchStat.getIdent()
        val cases = switchStat.getCases()

        val afterSwitchLabel = getNewLabel()

        val prevAfterLoopLabel = afterLoopLabel
        afterLoopLabel = afterSwitchLabel

        // Loads the switch element
        instructs.addAll(genLoadExpr(ident, reg1))

        // Loads the case instructions
        for (case in cases) {
            // Loads case values
            instructs.addAll(genLoadExpr(case.getCase(), reg2))
            // CMP r4, r5
            instructs.add(CmpInstruction(reg1, reg2))
            // BEQ label
            val caseLabel = getNewLabel()
            caseLabels.add(caseLabel)
            instructs.add(BranchInstruction(caseLabel, condition = WACCCondition.EQ))
        }

        instructs.add(BranchInstruction(afterSwitchLabel))

        for (i in cases.indices) {
            instructs.add(FuncNameInstruction(caseLabels[i]))
            instructs.addAll(translate(cases[i].getStat()))
        }

        instructs.add(FuncNameInstruction(afterSwitchLabel))

        afterLoopLabel = prevAfterLoopLabel

        return instructs
    }

    /**
     * Translate free statement.
     */
    fun translateFree(stat: WACCStatement): List<WACCInstruction> {
        assert(stat.getStatType() == WACCStatement.StatTypes.FREE)
        val instructs = mutableListOf<WACCInstruction>()

        val expr = stat.getExpr()
        instructs.addAll(genLoadExpr(expr!!, WACCRegister.R4))
        instructs.add(MovInstruction(toReg = WACCRegister.R0, fromReg = WACCRegister.R4))
        instructs.add(BranchInstruction(link = true, toLabel = CLibraryCalls.CallType.FREE_PAIR.toString()))
        cLibraryCalls.addCode(CLibraryCalls.CallType.FREE_PAIR)

        return instructs
    }

    /*********************
     * Utility Functions *
     *********************/

    private fun getNewLabel(): String {
        val labelString = "L" + labelCounter
        labelCounter++
        return labelString
    }

    /**
     * Generate an instruction to load a value for the given WACCIdentifier into the specified register.
     */
    private fun genLoadExpr(waccId: WACCIdentifier, reg: WACCRegister): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        val regToLoad = reg

        when (waccId) {
            is WACCIntLiter -> {
                val value = waccId.getValue()
                instructs.add(LdrInstruction(value.toString(), regToLoad))
            }
            is WACCBoolLiter -> {
                val value = waccId.getValue()
                instructs.add(MovInstruction(value.toInt(), regToLoad))
            }
            is WACCCharLiter -> {
                val value = waccId.getValue()
                instructs.add(MovInstruction(value, regToLoad))
            }
            is WACCStringLiter -> {
                val value: String = waccId.getValue()
                val label = dataSegment.addString(value)
                instructs.add(LdrInstruction(label, regToLoad))
            }
            is WACCIdent -> {
                val stackOffset = symbolTable.calculateStackOffset(waccId.toString())
                val isBoolOrCharType = waccId.getType() == WACCType.BOOL || waccId.getType() == WACCType.CHAR

                instructs.add(LdrInstruction(
                        WACCRegister.SP, regToLoad, stackOffset, singleByte = isBoolOrCharType, singleByteSigned = isBoolOrCharType))
            }
        }

        return instructs
    }

    /**
     * Generate an instruction to store the value of a register into stack memory with an offset.
     * i.e. STR fromReg, [SP, #offset]
     */
    private fun genStoreExpr(waccId: WACCIdentifier, fromReg: WACCRegister, offset: Int, rewrite: Boolean = false, addrAtReg: WACCRegister = WACCRegister.SP): List<StrInstruction> {
        val type = waccId.getType()

        if (type == WACCType.INT) {
            // STR r4, [sp, #offset]
            return listOf(StrInstruction(fromReg, addrAtReg, offset = offset, rewriteFrom = rewrite))
        } else if (type == WACCType.BOOL || type == WACCType.CHAR) {
            // STRB r4, [sp, #offset]
            return listOf(StrInstruction(fromReg, addrAtReg, offset = offset, singleByte = true, rewriteFrom = rewrite))
        } else if (waccId is WACCIdent) {
            val isBoolOrCharType = waccId.getType() == WACCType.BOOL || waccId.getType() == WACCType.CHAR
            return listOf(StrInstruction(
                    fromReg = fromReg, toAddrAtReg = addrAtReg, offset = offset, singleByte = isBoolOrCharType))
        } else if (type == WACCType.ARRAY) {
            return listOf(StrInstruction(
                    fromReg = fromReg, toAddrAtReg = addrAtReg, offset = offset))
        }
        return listOf()
    }

    /**
     * Helper function to generate code for accessing an array element.
     *
     * Takes an element expression `expr`.
     * Stores the address of the element in `arrayPtrReg`
     * Uses register `arrayIndexReg` to store the index for intermediate calculation
     * Uses `checkArrayBoundsArg1Reg` and `checkArrayBoundsArg2Reg` as arguments to p_check_array_bounds
     */
    fun genArrayElemHelper(expr: WACCArrayElem,
                           arrayPtrReg: WACCRegister,
                           arrayIndexReg: WACCRegister,
                           checkArrayBoundsArg1Reg: WACCRegister = WACCRegister.R0,
                           checkArrayBoundsArg2Reg: WACCRegister = WACCRegister.R1): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()

        val arrayIdent = expr.getArrayIdent()
        val arrayType = expr.getArrayType()
        val arrayIndices = expr.getIndices()
        val arrayOffset = symbolTable.calculateStackOffset(arrayIdent.toString())
        assert(arrayIndices.size in 1..2)

        // arrayPtrReg = SP + #(offset of variable on stack)
        instructs.add(AddInstruction(destReg = arrayPtrReg,
                operand1Reg = WACCRegister.SP,
                operand2Imm = arrayOffset))


        // array[expr], e.g. array[i]
        instructs.addAll(genLoadExpr(arrayIndices[0], arrayIndexReg))

        // arrayPtrReg = [arrayPtrReg] = location of array on the heap
        instructs.add(LdrInstruction(toReg = arrayPtrReg, fromAddrAtReg = arrayPtrReg))

        // Call p_check_array_bounds
        instructs.add(MovInstruction(toReg = checkArrayBoundsArg1Reg, fromReg = arrayIndexReg))
        instructs.add(MovInstruction(toReg = checkArrayBoundsArg2Reg, fromReg = arrayPtrReg))
        instructs.add(BranchInstruction(link = true, toLabel = RuntimeErrors.checkArrayBoundsLabel))
        runtimeErrors.addCheckArrayBounds()

        // arrayPtrReg = arrayPtrReg + sizeof(int)
        // i.e. skip over the integer storing the number of elements in the array, to the 0th element.
        instructs.add(AddInstruction(destReg = arrayPtrReg,
                operand1Reg = arrayPtrReg,
                operand2Imm = WACCType.INT.size))
        val leftShiftVal = log2(arrayType.size.toDouble()).toInt() // kind of a hack
        // arrayPtrReg = arrayPtrReg + arrayIndexReg * sizeof(array element size)
        // LSL if array element size is a power of two, e.g. LSL #2 same as x 4
        instructs.add(AddInstruction(destReg = arrayPtrReg,
                operand1Reg = arrayPtrReg,
                operand2Reg = arrayIndexReg,
                operand2RegShift = if (leftShiftVal > 0) LogicalLeftShift(leftShiftVal) else null))

        // For 2D arrays, process the next index (i.e. column index)
        if (arrayIndices.size == 2) {
            val colIndex = arrayIndices[1]
            // load column index
            instructs.addAll(genLoadExpr(colIndex, arrayIndexReg))
            // arrayPtrReg = [arrayPtrReg] = location of the selected row
            instructs.add(LdrInstruction(toReg = arrayPtrReg, fromAddrAtReg = arrayPtrReg))

            // Call p_check_array_bounds
            instructs.add(MovInstruction(toReg = checkArrayBoundsArg1Reg, fromReg = arrayIndexReg))
            instructs.add(MovInstruction(toReg = checkArrayBoundsArg2Reg, fromReg = arrayPtrReg))
            instructs.add(BranchInstruction(link = true, toLabel = RuntimeErrors.checkArrayBoundsLabel))
            runtimeErrors.addCheckArrayBounds()

            // arrayPtrReg = arrayPtrReg + sizeof(int)
            // i.e. skip over the integer storing the number of elements in the array, to the 0th element.
            instructs.add(AddInstruction(destReg = arrayPtrReg,
                    operand1Reg = arrayPtrReg,
                    operand2Imm = WACCType.INT.size))
            val leftShiftVal = log2(arrayType.size.toDouble()).toInt() // kind of a hack,
            // arrayPtrReg = arrayPtrReg + arrayIndexReg * sizeof(array element size)
            // LSL if array element size is a power of two, e.g. LSL #2 same as x 4
            instructs.add(AddInstruction(destReg = arrayPtrReg,
                    operand1Reg = arrayPtrReg,
                    operand2Reg = arrayIndexReg,
                    operand2RegShift = if (leftShiftVal > 0) LogicalLeftShift(leftShiftVal) else null))
        }

        // At this point in the assembly, arrayPtrReg = heap address of the array element
        return instructs
    }

    /**
     * Generate code for loading a value from an array element `expr` into register `toReg`.
     */
    fun genLoadArrayElem(expr: WACCArrayElem,
                         toReg: WACCRegister,
                         arrayPtrReg: WACCRegister,
                         arrayIndexReg: WACCRegister): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        instructs.addAll(genArrayElemHelper(expr, arrayPtrReg, arrayIndexReg))
        instructs.add(LdrInstruction(toReg = toReg, fromAddrAtReg = arrayPtrReg))
        return instructs
    }

    /**
     * Generate code for storing a value `rhs` from rhsValueReg into an array element `expr`.
     */
    fun genStoreArrayElem(expr: WACCArrayElem,
                          rhs: WACCIdentifier,
                          arrayPtrReg: WACCRegister,
                          arrayIndexReg: WACCRegister,
                          rhsValueReg: WACCRegister): List<WACCInstruction> {
        val instructs = mutableListOf<WACCInstruction>()
        instructs.addAll(genArrayElemHelper(expr, arrayPtrReg, arrayIndexReg))
        instructs.add(StrInstruction(singleByte = (rhs is WACCBoolLiter || rhs is WACCCharLiter),
                fromReg = rhsValueReg,
                toAddrAtReg = arrayPtrReg))
        return instructs
    }

    /**
     * Extend Boolean class to convert false/true to 0/1.
     */
    private fun Boolean.toInt(): Int {
        return if (this) {
            1
        } else {
            0
        }
    }

    /**
     * Return reference to data segment.
     */
    fun getDataSegment(): DataSegment {
        return dataSegment
    }

    /**
     * Return reference to current C library calls.
     */
    fun getCLibraryCalls(): CLibraryCalls {
        return cLibraryCalls
    }

    /**
     * Return reference to current runtime error code.
     */
    fun getRuntimeErrors(): RuntimeErrors {
        return runtimeErrors
    }
}
