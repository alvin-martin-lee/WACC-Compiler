package instr

import java.lang.StringBuilder

class LongMultiplyInstruction(private val operand1Reg: WACCRegister,
                              private val operand2Reg: WACCRegister,
                              private val resultRegLo: WACCRegister,
                              private val resultRegHi: WACCRegister,
                              private val signed: Boolean = true,
                              private val accumulate: Boolean = false,
                              private val updateFlags: Boolean = false,
                              private val condition: WACCCondition? = null): WACCInstruction {

    override fun toString(): String {
        val output = StringBuilder()
        output.append('\t')
        if (signed) {
            output.append('S')
        } else {
            output.append('U')
        }
        output.append('M')
        if (accumulate) {
            output.append("LA")
        } else {
            output.append("UL")
        }
        output.append('L')
        if (condition != null) output.append(condition)
        if (updateFlags) output.append('S')
        output.append(" $resultRegLo, $resultRegHi, $operand1Reg, $operand2Reg")
        return output.toString()
    }
}