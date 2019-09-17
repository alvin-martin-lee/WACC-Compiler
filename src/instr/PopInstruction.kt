package instr

import java.util.*

class PopInstruction(private val registers: List<WACCRegister>,
                     private val condition: WACCCondition? = null): WACCInstruction {
    constructor(register: WACCRegister, condition: WACCCondition? = null)
            : this(listOf(register), condition)

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\tPOP")
        if (condition != null) output.append(condition)
        output.append(" {${registers.joinToString(separator = ",")}}")
        return output.toString()
    }
}