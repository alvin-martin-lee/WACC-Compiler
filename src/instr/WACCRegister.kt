package instr

enum class WACCRegister {
    R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12,
    SP,   // == R13
    LR,   // == R14
    PC,   // == R15
    CPSR;

    fun next(): WACCRegister {
        return vals[(this.ordinal + 1) % vals.size]
    }

    companion object {
        private val vals = values()
    }
}