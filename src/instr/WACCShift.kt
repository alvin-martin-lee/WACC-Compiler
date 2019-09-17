package instr

interface WACCShift {
    override fun toString(): String
}

enum class ShiftType {
    ASR,
    LSL,
    LSR,
    ROR,
    RRX
}

abstract class NonExtendShift: WACCShift {
    private val bits: Int?
    private val bitsInReg: WACCRegister?

    constructor(bits: Int) {
        this.bits = bits
        this.bitsInReg = null
    }

    constructor(bitsInReg: WACCRegister) {
        this.bits = null
        this.bitsInReg = bitsInReg
    }

    abstract fun getType(): ShiftType

    override fun toString(): String {
        val output = StringBuilder()
        output.append(getType())
        if (bits != null) {
            output.append(" #$bits")
        } else {
            output.append(" $bitsInReg")
        }
        return output.toString()
    }
}

class ArithmeticRightShift: NonExtendShift {
    constructor(bits: Int): super(bits)
    constructor(bitsInReg: WACCRegister): super(bitsInReg)

    override fun getType(): ShiftType { return ShiftType.ASR }
}

class LogicalLeftShift: NonExtendShift {
    constructor(bits: Int): super(bits)
    constructor(bitsInReg: WACCRegister): super(bitsInReg)

    override fun getType(): ShiftType { return ShiftType.LSL }
}

class LogicalRightShift: NonExtendShift {
    constructor(bits: Int): super(bits)
    constructor(bitsInReg: WACCRegister): super(bitsInReg)

    override fun getType(): ShiftType { return ShiftType.LSR }
}

class RotateRightShift: NonExtendShift {
    constructor(bits: Int): super(bits)
    constructor(bitsInReg: WACCRegister): super(bitsInReg)

    override fun getType(): ShiftType { return ShiftType.ROR }
}

class RotateRightExtendShift(): WACCShift {
    override fun toString(): String {
        return "RRX"
    }
}
