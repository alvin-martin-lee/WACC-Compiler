package instr

abstract class MathInstruction: WACCInstruction {
    enum class OpType {
        ADD,   // Addition
        SUB,   // Subtraction
        RSB,   // Reverse subtraction
        AND,   // Logical AND
        ORR,   // Logical OR
        EOR    // Logical XOR
    }

    private val opType: OpType
    private val operand1Reg: WACCRegister
    private val operand2Reg: WACCRegister?
    private val operand2RegShift: WACCShift?
    private val operand2Imm: Long?
    private val destReg: WACCRegister
    private val updateFlags: Boolean
    private val condition: WACCCondition?

    private constructor(opType: OpType,
                        operand1Reg: WACCRegister,
                        operand2Reg: WACCRegister? = null,
                        operand2Imm: Long? = null,
                        destReg: WACCRegister = operand1Reg,
                        operand2RegShift: WACCShift? = null,
                        updateFlags: Boolean = false,
                        condition: WACCCondition? = null) {
        if (operand2Imm != null && operand2Imm > 65535) throw NotImplementedError("Cannot handle an immediate value > 65535 with $opType yet")
        this.opType = opType
        this.operand1Reg = operand1Reg
        this.operand2Reg = operand2Reg
        this.operand2RegShift = operand2RegShift
        this.operand2Imm = operand2Imm
        this.destReg = destReg
        this.updateFlags = updateFlags
        this.condition = condition
    }

    constructor(opType: OpType,
                operand1Reg: WACCRegister,
                operand2Reg: WACCRegister,
                destReg: WACCRegister = operand1Reg,
                operand2RegShift: WACCShift? = null,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(opType, operand1Reg, operand2Reg, null, destReg, operand2RegShift, updateFlags, condition)

    constructor(opType: OpType,
                operand1Reg: WACCRegister,
                operand2Imm: Long,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(opType, operand1Reg, null, operand2Imm, destReg, null, updateFlags, condition)

    override fun toString(): String {
        val output = StringBuilder()
        if (operand2Imm != null) {
            // Handle case of operand 2 as an immediate
            val immediateA = operand2Imm and 0xFF
            output.append("\t$opType")
            if (condition != null) output.append(condition)
            if (updateFlags) output.append('S')
            output.append(" $destReg, $operand1Reg, #$immediateA")

            // Check if operand 2 immediate value is larger than 8 bits
            if (operand2Imm > 255) {
                if (opType != OpType.ADD && opType != OpType.SUB) throw NotImplementedError("Cannot handle an immediate value > 255 with $opType yet")
                val immediateB = (operand2Imm and 0xFF00)
                output.append("\n\t$opType")
                if (condition != null) output.append(condition)
                if (updateFlags) output.append('S')
                output.append(" $destReg, $operand1Reg, #$immediateB")
            }
        } else {
            // Handle case of operand 2 as a register
            output.append("\t$opType")
            if (condition != null) output.append(condition)
            if (updateFlags) output.append('S')
            output.append(" $destReg, $operand1Reg, $operand2Reg")
            if (operand2RegShift != null) output.append(", $operand2RegShift")
        }
        return output.toString()
    }

    private fun validPattern(immediate: Int): Boolean {
        var currentNum = immediate

        while (currentNum % 2 == 0) {
            currentNum /= 2
        }

        return currentNum <= 256
    }

//    private fun getLargestImmediate(immediate: Int)
}

class AddInstruction: MathInstruction {
    constructor(operand1Reg: WACCRegister,
                operand2Reg: WACCRegister,
                destReg: WACCRegister = operand1Reg,
                operand2RegShift: WACCShift? = null,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.ADD, operand1Reg, operand2Reg, destReg, operand2RegShift, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Int,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(operand1Reg, operand2Imm.toLong(), destReg, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Long,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.ADD, operand1Reg, operand2Imm, destReg, updateFlags, condition)
}

class SubInstruction: MathInstruction {
    constructor(operand1Reg: WACCRegister,
                operand2Reg: WACCRegister,
                destReg: WACCRegister = operand1Reg,
                operand2RegShift: WACCShift? = null,
                reverse: Boolean = false,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(if (reverse) OpType.RSB else OpType.SUB, operand1Reg, operand2Reg, destReg, operand2RegShift, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Int,
                destReg: WACCRegister = operand1Reg,
                reverse: Boolean = false,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(operand1Reg, operand2Imm.toLong(), destReg, reverse, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Long,
                destReg: WACCRegister = operand1Reg,
                reverse: Boolean = false,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(if (reverse) OpType.RSB else OpType.SUB, operand1Reg, operand2Imm, destReg, updateFlags, condition)
}

class AndInstruction: MathInstruction {
    constructor(operand1Reg: WACCRegister,
                operand2Reg: WACCRegister,
                destReg: WACCRegister = operand1Reg,
                operand2RegShift: WACCShift? = null,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.AND, operand1Reg, operand2Reg, destReg, operand2RegShift, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Int,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(operand1Reg, operand2Imm.toLong(), destReg, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Long,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.AND, operand1Reg, operand2Imm, destReg, updateFlags, condition)
}

class OrrInstruction: MathInstruction {
    constructor(operand1Reg: WACCRegister,
                operand2Reg: WACCRegister,
                destReg: WACCRegister = operand1Reg,
                operand2RegShift: WACCShift? = null,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.ORR, operand1Reg, operand2Reg, destReg, operand2RegShift, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Int,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(operand1Reg, operand2Imm.toLong(), destReg, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Long,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.ORR, operand1Reg, operand2Imm, destReg, updateFlags, condition)
}

class EorInstruction: MathInstruction {
    constructor(operand1Reg: WACCRegister,
                operand2Reg: WACCRegister,
                destReg: WACCRegister = operand1Reg,
                operand2RegShift: WACCShift? = null,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.EOR, operand1Reg, operand2Reg, destReg, operand2RegShift, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Int,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : this(operand1Reg, operand2Imm.toLong(), destReg, updateFlags, condition)

    constructor(operand1Reg: WACCRegister,
                operand2Imm: Long,
                destReg: WACCRegister = operand1Reg,
                updateFlags: Boolean = false,
                condition: WACCCondition? = null)
            : super(OpType.EOR, operand1Reg, operand2Imm, destReg, updateFlags, condition)
}
