package wacc

import instr.FuncNameInstruction
import instr.InlineLabelInstruction
import instr.LabelInstruction
import instr.WACCInstruction

/**
 * Data section. Currently it only stores string literals.
 */
class DataSegment(private val stringLiterals: StringLiterals) {

    /**
     * Add a string literal to the data section.
     */
    fun addString(string: String): String {
        return stringLiterals.add(string)
    }

    /**
     * Translate the data section into assembly code.
     */
    fun translate(): List<WACCInstruction> {
        if (stringLiterals.strings.size == 0) return emptyList()
        val instructions = mutableListOf<WACCInstruction>()
        instructions.add(LabelInstruction("data"))
        val strings = stringLiterals.translateAll()
        instructions.addAll(strings)
        return instructions
    }
}

/**
 * Contains string literals to be stored in the .data section of the code.
 */
class StringLiterals {
    val strings: MutableList<String> = mutableListOf()

    /**
     * Add a string and returns its label of the format "msg_<int>".
     */
    fun add(string: String): String {
        strings.add(string)
        return "msg_${strings.size - 1}"
    }

    /**
     * Return string literals listed in ARM format
     *
     * Format:
     * <string label>:
     *     .word <length of string>
     *     .ascii "<actual string literal>"
     */
    fun translateAll(): List<WACCInstruction> {
        val instructions = mutableListOf<WACCInstruction>()
        for ((index, string) in strings.withIndex()) {
            instructions.add(FuncNameInstruction("msg_$index"))
            instructions.add(InlineLabelInstruction("word ${string.length}"))

            // display escaped characters in full
            val newString = escapeString(string)
            instructions.add(InlineLabelInstruction("ascii \"$newString\""))
        }
        return instructions
    }
}