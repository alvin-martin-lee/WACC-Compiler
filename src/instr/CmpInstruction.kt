package instr

class CmpInstruction: WACCInstruction {
    private val valueOfReg: WACCRegister
    private val withValueOfReg: WACCRegister?
    private val withValueOfImm: Int?
    private val withValueOfRegShift: WACCShift?
    private val condition: WACCCondition?

    constructor(valueOfReg: WACCRegister,
                withValueOfReg: WACCRegister,
                withValueOfRegShift: WACCShift? = null,
                condition: WACCCondition? = null) {
        this.valueOfReg = valueOfReg
        this.withValueOfReg = withValueOfReg
        this.withValueOfImm = null
        this.withValueOfRegShift = withValueOfRegShift
        this.condition = condition
    }

    constructor(valueOfReg: WACCRegister,
                withValueOfImm: Int,
                condition: WACCCondition? = null) {
        this.valueOfReg = valueOfReg
        this.withValueOfReg = null
        this.withValueOfImm = withValueOfImm
        this.withValueOfRegShift = null
        this.condition = condition
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\tCMP")
        if (condition != null) output.append(condition)
        output.append(" $valueOfReg, ")
        if (withValueOfReg != null) {
            output.append(withValueOfReg)
            if (withValueOfRegShift != null) output.append(", $withValueOfRegShift")
        } else {
            output.append("#$withValueOfImm")
        }
        return output.toString()
    }
}