package wacc

import instr.*

/**
 * Automatically generate a call to runtime errors and error-checkers if needed.
 */
class RuntimeErrors(val codeGenerator: WACCCodeGenerator) {

    private val ERROR_EXIT_CODE = -1
    private var runtimeError: List<WACCInstruction>? = null
    private var overflowError: List<WACCInstruction>? = null
    private var divZeroError: List<WACCInstruction>? = null
    private var checkArrayBounds: List<WACCInstruction>? = null
    private var nullRefError: List<WACCInstruction>? = null

    /* labels for each type of error. */
    companion object {
        val throwRuntimeErrorLabel = "p_throw_runtime_error"
        val throwOverflowErrorLabel = "p_throw_overflow_error"
        val divZeroCheckLabel = "p_check_divide_by_zero"
        val checkArrayBoundsLabel = "p_check_array_bounds"
        val nullRefLabel = "p_check_null_pointer"
        val exitLabel = "exit"
    }

    /* Type of each runtime error and corresponding error message. */
    enum class RuntimeErrorType(val message: String) {
        ARRAY_INDEX_OUT_OF_BOUNDS_LARGE("ArrayIndexOutOfBoundsError: index too large\n" + 0.toChar()),
        ARRAY_INDEX_OUT_OF_BOUNDS_NEG("ArrayIndexOutOfBoundsError: negative index\n" + 0.toChar()),
        NULL_REFERENCE("NullReferenceError: dereference a null reference\n" + 0.toChar()),
        OVERFLOW_ERROR("OverflowError: the result is too small/large to store in a 4-byte signed-integer.\n"),
        DIVIDE_BY_ZERO("DivideByZeroError: divide or modulo by zero\n" + 0.toChar());

        override fun toString(): String {
            return message
        }
    }

    /**
     * Add a run time error.
     *
     * For example, a "free x" statement translation makes CLibraryCalls auto-generate a
     * "p_free_pair" function. Inside "p_free_pair", it has to check if the user is trying
     * to free a null pointer and raise a runtime error accordingly.
     * To do this, it passes an error message (e.g. msg_0) and calls "p_throw_runtime_error".
     *
     * Currently it is the caller's responsibility to pass the correct error string before branching
     * to the runtime error function.
     */
    fun addThrowRuntimeError() {
        if (runtimeError == null) {
            // p_throw_runtime_error:
            //   BL p_print_string
            //   MOV r0, #-1
            //   BL exit
            runtimeError = listOf(
                    FuncNameInstruction(throwRuntimeErrorLabel),
                    BranchInstruction(CLibraryCalls.CallType.PRINT_STRING.toString(), link = true),
                    MovInstruction(toReg = WACCRegister.R0, fromImmInt = ERROR_EXIT_CODE),
                    BranchInstruction(exitLabel, link = true))
            codeGenerator.getCLibraryCalls().addCode(CLibraryCalls.CallType.PRINT_STRING)
        }
    }

    /**
     * Overflow error. Special case of runtime error.
     */
    fun addOverflowError() {
        // p_throw_overflow_error:
        //   LDR r0, =msg_0
        //   BL p_throw_runtime_error
        // p_throw_runtime_error:
        //   ...
        if (overflowError == null) {
            val errorMsg = codeGenerator.getDataSegment().addString(RuntimeErrorType.OVERFLOW_ERROR.toString())
            overflowError = listOf(
                    FuncNameInstruction(throwOverflowErrorLabel),
                    LdrInstruction(fromLabel = errorMsg, toReg = WACCRegister.R0),
                    BranchInstruction(link = true, toLabel = throwRuntimeErrorLabel)
            )
        }
        addThrowRuntimeError()
    }

    /**
     * Add divide-by-zero checking code (p_check_divide_by_zero).
     */
    fun addDivByZeroCheck() {
        // p_check_divide_by_zero:
        //   PUSH {lr}
        //   CMP r1, #0
        //   LDREQ r0, =msg_0
        //   BLEQ p_throw_runtime_error
        //   POP {pc}
        if (divZeroError == null) {
            val errorMsg = codeGenerator.getDataSegment().addString(RuntimeErrorType.DIVIDE_BY_ZERO.toString())
            divZeroError = listOf(
                    FuncNameInstruction(divZeroCheckLabel),
                    PushInstruction(WACCRegister.LR),
                    CmpInstruction(WACCRegister.R1, 0),
                    LdrInstruction(fromLabel = errorMsg, toReg = WACCRegister.R0, condition = WACCCondition.EQ),
                    BranchInstruction(throwRuntimeErrorLabel, true, WACCCondition.EQ),
                    PopInstruction(WACCRegister.PC)
            )
        }
        addThrowRuntimeError()
    }

    /**
     * Add code for checking array access bounds (p_check_array_bounds).
     */
    fun addCheckArrayBounds() {
        if (checkArrayBounds == null) {
            val data = codeGenerator.getDataSegment()
            val negMsgLabel = data.addString(RuntimeErrorType.ARRAY_INDEX_OUT_OF_BOUNDS_NEG.toString())
            val largeMsgLabel = data.addString(RuntimeErrorType.ARRAY_INDEX_OUT_OF_BOUNDS_LARGE.toString())

            // p_check_array_bounds:
            //   PUSH {lr}
            //   CMP r0, #0
            //   LDRLT r0, =msg_0
            //   BLLT p_throw_runtime_error
            //   LDR r1, [r1]
            //   CMP r0, r1
            //   LDRCS r0, =msg_1
            //   BLCS p_throw_runtime_error
            //   POP {pc}

            checkArrayBounds = listOf(
                    FuncNameInstruction(checkArrayBoundsLabel),
                    PushInstruction(WACCRegister.LR),
                    CmpInstruction(valueOfReg = WACCRegister.R0, withValueOfImm = 0),
                    LdrInstruction(fromLabel = negMsgLabel, toReg = WACCRegister.R0, condition = WACCCondition.LT),
                    BranchInstruction(toLabel = throwRuntimeErrorLabel, link = true, condition = WACCCondition.LT),
                    LdrInstruction(fromAddrAtReg = WACCRegister.R1, toReg = WACCRegister.R1),
                    CmpInstruction(valueOfReg = WACCRegister.R0, withValueOfReg = WACCRegister.R1),
                    LdrInstruction(fromLabel = largeMsgLabel, toReg = WACCRegister.R0, condition = WACCCondition.CS),
                    BranchInstruction(toLabel = throwRuntimeErrorLabel, link = true, condition = WACCCondition.CS),
                    PopInstruction(WACCRegister.PC)
            )
            addThrowRuntimeError()
        }
    }

    /**
     * Add null reference check.
     */
    fun addNullReferenceCheck() {
        if (nullRefError == null) {
            val errorMsgLabel = codeGenerator.getDataSegment().addString(RuntimeErrorType.NULL_REFERENCE.toString())
            nullRefError = listOf(
                    FuncNameInstruction(nullRefLabel),
                    PushInstruction(WACCRegister.LR),
                    CmpInstruction(WACCRegister.R0, 0),
                    LdrInstruction(condition = WACCCondition.EQ, fromLabel = errorMsgLabel, toReg = WACCRegister.R0),
                    BranchInstruction(link = true, condition = WACCCondition.EQ, toLabel = throwRuntimeErrorLabel),
                    PopInstruction(WACCRegister.PC)
            )
        }
        addThrowRuntimeError()
    }

    /**
     * Generate all required error-checking and error-generating functions.
     */
    fun translate(): List<WACCInstruction> {
        val instructions = mutableListOf<WACCInstruction>()
        overflowError?.let { instructions.addAll(it) }
        divZeroError?.let { instructions.addAll(it) }
        checkArrayBounds?.let { instructions.addAll(it) }
        nullRefError?.let {instructions.addAll(it)}
        runtimeError?.let { instructions.addAll(it) }
        return instructions
    }
}
