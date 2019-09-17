package instr;

class BranchInstruction(private val toLabel: String,
                        private val link: Boolean = false,
                        private val condition: WACCCondition? = null): WACCInstruction {

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\tB")
        if (link) output.append('L')
        if (condition != null) output.append(condition)
        output.append(" $toLabel")
        return output.toString()
    }
}
