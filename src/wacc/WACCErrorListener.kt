package wacc

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class WACCErrorListener : BaseErrorListener() {
    companion object {
        const val SYNTAX_ERROR = 100
        const val SEMANTIC_ERROR = 200
    }

    override fun syntaxError(recognizer: Recognizer<*, *>?,
                             offendingSymbol: Any?,
                             line: Int,
                             charPositionInLine: Int,
                             msg: String?,
                             e: RecognitionException?) {
        System.out.println("100 - Syntax Error at line $line, position $charPositionInLine.")
        if (msg != null && msg.isNotEmpty()) {
            System.out.println(msg)
        }
        System.exit(SYNTAX_ERROR)
    }

    fun visitorError(type: WACCErrorType,
                     msg: String,
                     context: ParserRuleContext): WACCIdentifier {
        val line = context.getStart().line
        val charPositionInLine = context.getStart().charPositionInLine
        when (type) {
            WACCErrorType.SYNTAX -> {
                System.out.println("100 - Syntax Error at line $line, position $charPositionInLine.")
                System.out.println(msg)
                System.exit(SYNTAX_ERROR)
            }
            WACCErrorType.SEMANTIC -> {
                System.out.println("200 - Semantic Error at line $line, position $charPositionInLine.")
                System.out.println(msg)
                System.exit(SEMANTIC_ERROR)
            }
        }
        return WACCIdentifier(WACCType.NONE)
    }
}

enum class WACCErrorType {
    SYNTAX, SEMANTIC
}