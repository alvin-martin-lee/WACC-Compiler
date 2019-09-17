package instr

class LdrInstruction: WACCInstruction {
    private val fromAddrAtReg: WACCRegister?
    private val fromLabel: String?
    private val toReg: WACCRegister
    private val offset: Int
    private val rewriteFrom: Boolean
    private val singleByte: Boolean
    private val singleByteSigned: Boolean
    private val condition: WACCCondition?

    constructor(fromLabel: String,
                toReg: WACCRegister,
                singleByte: Boolean = false,
                singleByteSigned: Boolean = false,
                condition: WACCCondition? = null) {
        this.fromAddrAtReg = null
        this.fromLabel = fromLabel
        this.toReg = toReg
        this.offset = 0
        this.rewriteFrom = false
        this.singleByte = singleByte
        this.singleByteSigned = singleByteSigned
        this.condition = condition
    }

    constructor(fromAddrAtReg: WACCRegister,
                toReg: WACCRegister,
                offset: Int = 0,
                rewriteFrom: Boolean = false,
                singleByte: Boolean = false,
                singleByteSigned: Boolean = false,
                condition: WACCCondition? = null) {
        this.fromAddrAtReg = fromAddrAtReg
        this.fromLabel = null
        this.toReg = toReg
        this.offset = offset
        this.rewriteFrom = rewriteFrom
        this.singleByte = singleByte
        this.singleByteSigned = singleByteSigned
        this.condition = condition
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\tLDR")
        if (condition != null) output.append(condition)
        if (singleByte) {
            if (singleByteSigned) output.append('S')
            output.append('B')
        }
        output.append(" $toReg, ")
        if (fromAddrAtReg != null) {
            output.append("[$fromAddrAtReg")
            if (offset != 0) output.append(", #$offset")
            output.append("]")
            if (rewriteFrom) output.append('!')
        } else {
            output.append("=$fromLabel")
        }
        return output.toString()
    }
}