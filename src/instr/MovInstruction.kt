package instr

class MovInstruction: WACCInstruction {
    private val fromReg: WACCRegister?
    private val fromImmInt: Int?
    private val fromImmChar: Char?
    private val toReg: WACCRegister
    private val condition: WACCCondition?

    constructor(fromReg: WACCRegister, toReg: WACCRegister, condition: WACCCondition? = null) {
        this.fromReg = fromReg
        this.fromImmInt = null
        this.fromImmChar = null
        this.toReg = toReg
        this.condition = condition
    }

    constructor(fromImmInt: Int, toReg: WACCRegister, condition: WACCCondition? = null) {
        this.fromReg = null
        this.fromImmInt = fromImmInt
        this.fromImmChar = null
        this.toReg = toReg
        this.condition = condition
    }

    constructor(fromImmChar: Char, toReg: WACCRegister, condition: WACCCondition? = null) {
        this.fromReg = null
        this.fromImmInt = null
        this.fromImmChar = fromImmChar
        this.toReg = toReg
        this.condition = condition
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\tMOV")
        if (condition != null) output.append(condition)
        output.append(" $toReg, ")
        if (fromReg != null) {
            output.append(fromReg)
        } else if (fromImmInt != null) {
            output.append("#$fromImmInt")
        } else {
            output.append("#'$fromImmChar'")
        }
        return output.toString()
    }
}