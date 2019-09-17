package wacc

import instr.*

/**
 * Contains code for C library calls to be added to the end of the assembly file.
 *
 * C Library calls are needed for some statements (e.g. "read x" requires scanf).
 * Code to call them are automatically generated when the main generator
 * encounters such statements. Code for runtime errors may also be generated,
 * e.g. freeing a pair might result in a null reference error. Generated code
 * have labels in the format "p_{name}".
 *
 * According to the reference compiler, p_print_ln prints a new line only.
 * A statement like 'println "hello"' requires generating
 * p_print_string followed by p_print_ln
 *
 */
class CLibraryCalls(val codeGenerator: WACCCodeGenerator) {

    /* C Library functions that will be called in the assembly code. */
    enum class CLibFunctions {
        PRINTF,
        SCANF,
        FFLUSH,
        PUTS,
        PUTCHAR,
        MALLOC,
        FREE,
        STRCAT,
        STRSTR,
        POW,
        MAX,
        MIN,
        SQUARE,
        CUBE,
        HYPO;

        override fun toString(): String {
            return super.toString().toLowerCase()
        }
    }

    /* Type of generated functions */
    enum class CallType {
        READ_INT,
        READ_CHAR,
        PRINT_INT,
        PRINT_BOOL,
        PRINT_STRING,
        PRINT_REFERENCE,
        PRINT_LN,
        FREE_ARRAY,
        FREE_PAIR,
        CONCAT_STRINGS,
        STRING_CONTAINS,
        POWER,
        MAX,
        MIN,
        SQUARE,
        CUBE,
        HYPO;

        override fun toString(): String {
            return "p_${super.toString().toLowerCase()}"
        }
    }

    /* Stores all auto-generated call code. */
    private val cLibraryCalls: LinkedHashMap<CallType, List<WACCInstruction>> = LinkedHashMap()

    /**
     * Add a C Library function-calling section.
     */
    fun addCode(type: CallType) {
        if (cLibraryCalls.containsKey(type)) {
            return
        }
        val instructions = mutableListOf<WACCInstruction>()
        val callLabel = FuncNameInstruction(type.toString())
        val body = when (type) {
            CallType.READ_INT,
            CallType.READ_CHAR -> generateRead(type)
            CallType.PRINT_STRING -> generatePrintString()
            CallType.PRINT_LN -> generatePrintln()
            CallType.PRINT_INT -> generatePrintInt()
            CallType.PRINT_BOOL -> generatePrintBool()
            CallType.PRINT_REFERENCE -> generatePrintReference()
            CallType.FREE_PAIR -> generateFreePair()
            CallType.CONCAT_STRINGS -> generateConcatStrings()
            CallType.STRING_CONTAINS -> generateStringContains()
            CallType.POWER -> generatePower()
            else -> throw NotImplementedError()
        }
        instructions.add(callLabel)
        instructions.addAll(body)
        cLibraryCalls[type] = instructions
    }

    /**
     * Code to check if a string is a substring of another.
     *
     * strstr(haystack, needle) returns a non-null value if needle is a substring of haystack.
     */
    private fun generateStringContains(): Collection<WACCInstruction> {
        // r0: string
        // r1: substring
        // ADD r0, r0, #4
        // ADD r1, r1, #4
        // BL strstr
        return functionWrapper(listOf(
                AddInstruction(WACCRegister.R0, 4),
                AddInstruction(WACCRegister.R1, 4),
                BranchInstruction(link=true, toLabel = CLibFunctions.STRSTR.toString())
        ))
    }

    /**
     * Code for concatenating two strings.
     */
    private fun generateConcatStrings(): Collection<WACCInstruction> {
        // r0: first string
        // r1: second string
        // ADD r0, r0, #4
        // ADD r1, r1, #4
        // BL strcat
        return functionWrapper(listOf(
                AddInstruction(WACCRegister.R0, 4),
                AddInstruction(WACCRegister.R1, 4),
                BranchInstruction(link=true, toLabel = CLibFunctions.STRCAT.toString())
        ))
    }

    /**
     * Code for freeing pair memory.
     */
    private fun generateFreePair(): Collection<WACCInstruction> {
        // CMP r0, #0
        // LDREQ r0, =msg_0
        // BEQ p_throw_runtime_error
        // PUSH {r0}
        // LDR r0, [r0]
        // BL free
        // LDR r0, [sp]
        // LDR r0, [r0, #4]
        // BL free
        // POP {r0}
        // BL free
        val errorLabel = codeGenerator
                .getDataSegment()
                .addString(RuntimeErrors.RuntimeErrorType.NULL_REFERENCE.toString())
        codeGenerator.getRuntimeErrors().addThrowRuntimeError()
        return functionWrapper(listOf(
                CmpInstruction(WACCRegister.R0, 0),
                LdrInstruction(condition = WACCCondition.EQ, toReg = WACCRegister.R0, fromLabel = errorLabel),
                BranchInstruction(condition = WACCCondition.EQ, toLabel = RuntimeErrors.throwRuntimeErrorLabel),
                PushInstruction(WACCRegister.R0),
                LdrInstruction(toReg = WACCRegister.R0, fromAddrAtReg = WACCRegister.R0),
                BranchInstruction(link = true, toLabel = CLibraryCalls.CLibFunctions.FREE.toString()),
                LdrInstruction(toReg = WACCRegister.R0, fromAddrAtReg = WACCRegister.SP),
                LdrInstruction(toReg = WACCRegister.R0, fromAddrAtReg = WACCRegister.R0, offset = 4),
                BranchInstruction(link = true, toLabel = CLibraryCalls.CLibFunctions.FREE.toString()),
                PopInstruction(WACCRegister.R0),
                BranchInstruction(link = true, toLabel = CLibraryCalls.CLibFunctions.FREE.toString())
        ))
    }

    /*
     * Code for printing boolean values "true" or "false".
     */
    private fun generatePrintBool(): List<WACCInstruction> {
        val trueString = "true" + 0.toChar()
        val falseString = "false" + 0.toChar()
        val trueLabel = codeGenerator.getDataSegment().addString(trueString)
        val falseLabel = codeGenerator.getDataSegment().addString(falseString)
        // CMP r0, #0
        // LDRNE r0, =msg_0
        // LDREQ r0, =msg_1
        // ADD r0, r0, #4
        // BL printf
        // MOV r0, #0
        // BL fflush
        val instructions = mutableListOf<WACCInstruction>(
                CmpInstruction(valueOfReg = WACCRegister.R0, withValueOfImm = 0),
                LdrInstruction(fromLabel = trueLabel, toReg = WACCRegister.R0, condition = WACCCondition.NE),
                LdrInstruction(fromLabel = falseLabel, toReg = WACCRegister.R0, condition = WACCCondition.EQ),
                AddInstruction(operand1Reg = WACCRegister.R0,operand2Imm = 4, destReg = WACCRegister.R0),
                BranchInstruction(toLabel = CLibraryCalls.CLibFunctions.PRINTF.toString(), link = true),
                MovInstruction(fromImmInt = 0, toReg = WACCRegister.R0),
                BranchInstruction(toLabel = CLibFunctions.FFLUSH.toString(), link = true)
        )
        return functionWrapper(instructions)
    }

    /**
     * Code for reading a value from stdin based on the given type.
     */
    private fun generateRead(type: CallType): List<WACCInstruction> {
        val stringFormat: String = when (type) {
            CallType.READ_INT -> "%d" + 0.toChar()
            CallType.READ_CHAR -> " %c" + 0.toChar()
            else -> throw Exception("Can't generate code for non-read type")
        }
        val stringFormatLabel = codeGenerator.getDataSegment().addString(stringFormat)
        // MOV r1, r0
        // LDR r0, =stringFormatLabel
        // ADD r0, r0, #4
        // BL scanf
        return functionWrapper(listOf(
                MovInstruction(WACCRegister.R0, WACCRegister.R1),
                LdrInstruction(stringFormatLabel, WACCRegister.R0),
                AddInstruction(WACCRegister.R0, 4),
                BranchInstruction(CLibFunctions.SCANF.toString(), true)
        ))
    }

    /**
     * Code for printing a string (no newline).
     */
    private fun generatePrintString(): List<WACCInstruction> {
        val instructions: MutableList<WACCInstruction> = mutableListOf()
        val stringFormat: String = "%.*s" + 0.toChar()
        val stringFormatLabel = codeGenerator.getDataSegment().addString(stringFormat)

        // LDR r1, [r0]
        // ADD r2, r0, #4
        // LDR r0, =stringFormatLabel
        // ADD r0, r0, #4
        // BL printf
        // MOV r0, #0
        // BL fflush
        return functionWrapper(listOf(
                LdrInstruction(fromAddrAtReg = WACCRegister.R0, toReg = WACCRegister.R1),
                AddInstruction(WACCRegister.R0, 4, WACCRegister.R2),
                LdrInstruction(fromLabel = stringFormatLabel, toReg = WACCRegister.R0),
                AddInstruction(WACCRegister.R0, 4),
                BranchInstruction(CLibFunctions.PRINTF.toString(), true),
                MovInstruction(0, WACCRegister.R0),
                BranchInstruction(CLibFunctions.FFLUSH.toString(), true)
        ))
    }

    /**
     * Code for printing an integer (without newline).
     */
    private fun generatePrintInt(): List<WACCInstruction> {
        val stringFormat: String = "%d" + 0.toChar()
        val stringFormatLabel = codeGenerator.getDataSegment().addString(stringFormat)

        // MOV r1, r0
        // LDR r0, =stringFormatLabel
        // ADD r0, r0, #4
        // BL printf
        // MOV r0, #0
        // BL fflush
        return functionWrapper(listOf(
                MovInstruction(WACCRegister.R0, WACCRegister.R1),
                LdrInstruction(stringFormatLabel, WACCRegister.R0),
                AddInstruction(WACCRegister.R0, 4),
                BranchInstruction(CLibFunctions.PRINTF.toString(), true),
                MovInstruction(0, WACCRegister.R0),
                BranchInstruction(CLibFunctions.FFLUSH.toString(), true)
        ))
    }

    /**
     * Code for printing a newline character.
     */
    private fun generatePrintln(): List<WACCInstruction> {
        val stringFormat: String = "" + 0.toChar()
        val stringFormatLabel = codeGenerator.getDataSegment().addString(stringFormat)
        // LDR r0, =msg_1
        // ADD r0, r0, #4
        // BL puts
        // MOV r0, #0
        // BL fflush
        return functionWrapper(listOf(
                LdrInstruction(stringFormatLabel, WACCRegister.R0),
                AddInstruction(WACCRegister.R0, 4),
                BranchInstruction(CLibFunctions.PUTS.toString(), true),
                MovInstruction(0, WACCRegister.R0),
                BranchInstruction(CLibFunctions.FFLUSH.toString(), true)
        ))
    }


    /**
     * Code for printing a reference.
     */
    private fun generatePrintReference(): List<WACCInstruction> {
        val stringFormat = "%p" + 0.toChar()
        val formatLabel = codeGenerator.getDataSegment().addString(stringFormat)
        return functionWrapper(listOf(
                // MOV r1, r0
                // LDR r0, =msg_2
                // ADD r0, r0, #4
                // BL printf
                // MOV r0, #0
                // BL fflush
                MovInstruction(toReg = WACCRegister.R1, fromReg = WACCRegister.R0),
                LdrInstruction(toReg = WACCRegister.R0, fromLabel = formatLabel),
                AddInstruction(destReg = WACCRegister.R0, operand1Reg = WACCRegister.R0, operand2Imm = 4),
                BranchInstruction(link = true, toLabel = CLibFunctions.PRINTF.toString()),
                MovInstruction(toReg = WACCRegister.R0, fromImmInt = 0),
                BranchInstruction(link = true, toLabel = CLibFunctions.FFLUSH.toString())
        ))
    }

    /**
     * Code for freeing an array from heap memory.
     */
    private fun generateFreeArray(): List<WACCInstruction> {
        val errorMsg = RuntimeErrors.RuntimeErrorType.NULL_REFERENCE.toString()
        val errorLabel = codeGenerator.getDataSegment().addString(errorMsg)
        codeGenerator.getRuntimeErrors().addThrowRuntimeError()
        // CMP r0, #0
        // LDREQ r0, =msg_0
        // BEQ p_throw_runtime_error
        // BL free
        return functionWrapper(listOf(
                CmpInstruction(withValueOfImm = 0, valueOfReg = WACCRegister.R0),
                LdrInstruction(fromLabel = errorLabel, toReg = WACCRegister.R0, condition = WACCCondition.EQ),
                BranchInstruction(toLabel = RuntimeErrors.throwRuntimeErrorLabel, condition = WACCCondition.EQ),
                BranchInstruction(CLibFunctions.FREE.toString())
        ))
    }

    private fun generatePower(): List<WACCInstruction> {
        return functionWrapper(listOf(
                SubInstruction(WACCRegister.SP, 4),
                LdrInstruction("1", WACCRegister.R4),
                StrInstruction(WACCRegister.R4, WACCRegister.SP),
                BranchInstruction("Pow1"),
                FuncNameInstruction("Pow2"),
                LdrInstruction(WACCRegister.SP, WACCRegister.R4),
                LdrInstruction(WACCRegister.SP, WACCRegister.R5, 8),
                LongMultiplyInstruction(WACCRegister.R4, WACCRegister.R5, WACCRegister.R4, WACCRegister.R5),
                CmpInstruction(WACCRegister.R5, WACCRegister.R4, ArithmeticRightShift(31)),
                BranchInstruction("p_throw_overflow_error", true, WACCCondition.NE),
                StrInstruction(WACCRegister.R4, WACCRegister.SP),
                LdrInstruction(WACCRegister.SP, WACCRegister.R4, 12),
                LdrInstruction("1", WACCRegister.R5),
                SubInstruction(WACCRegister.R4, WACCRegister.R5),
                BranchInstruction("p_throw_overflow_error", true, WACCCondition.VS),
                StrInstruction(WACCRegister.R4, WACCRegister.SP, 12),
                FuncNameInstruction("Pow1"),
                LdrInstruction(WACCRegister.SP, WACCRegister.R4, 12),
                LdrInstruction("0", WACCRegister.R5),
                CmpInstruction(WACCRegister.R4, WACCRegister.R5),
                MovInstruction(1, WACCRegister.R4, WACCCondition.GT),
                MovInstruction(0, WACCRegister.R4, WACCCondition.LE),
                CmpInstruction(WACCRegister.R4, 1),
                BranchInstruction("Pow2"),
                LdrInstruction(WACCRegister.SP, WACCRegister.R4),
                MovInstruction(WACCRegister.R4, WACCRegister.R0),
                AddInstruction(WACCRegister.SP, 4),
                PopInstruction(WACCRegister.PC)
                ))
    }

    /**
     * Convenience function for adding PUSH {lr} and POP {pc} around a function body.
     */
    private fun functionWrapper(instructions: List<WACCInstruction>): List<WACCInstruction> {
        // PUSH {lr}
        // <body>
        // POP {pc}
        return listOf(PushInstruction(WACCRegister.LR)) + instructions + listOf(PopInstruction(WACCRegister.PC))
    }

    /**
     * Retrieve all the auto-generated C Library calls as a WACCInstruction list.
     */
    fun translate(): List<WACCInstruction> {
        val instructions = mutableListOf<WACCInstruction>()
        for ((_, value) in cLibraryCalls) {
            instructions.addAll(value)
        }
        return instructions
    }
}