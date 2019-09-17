package instr

abstract class TextInstruction(private val text: String): WACCInstruction {
    override fun toString(): String {
        return text
    }
}

class FuncNameInstruction(funcName: String): TextInstruction("$funcName:")

class LabelInstruction(label: String): TextInstruction(".$label")

class InlineLabelInstruction(label: String): TextInstruction("\t.$label")
