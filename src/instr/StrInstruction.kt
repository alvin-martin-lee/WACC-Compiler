package instr

class StrInstruction(private val fromReg: WACCRegister,
                     private val toAddrAtReg: WACCRegister,
                     private val offset: Int = 0,
                     private val rewriteFrom: Boolean = false,
                     private val singleByte: Boolean = false,
                     private val condition: WACCCondition? = null) : WACCInstruction {

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\tSTR")
        if (condition != null) output.append(condition)
        if (singleByte) output.append('B')
        output.append(" $fromReg, [$toAddrAtReg")
        if (offset != 0) output.append(", #$offset")
        output.append("]")
        if (rewriteFrom) output.append('!')
        return output.toString()
    }
}