import antlr.WACCLexer
import antlr.WACCParser
import java.io.IOException
import org.antlr.v4.runtime.*
import wacc.*
import java.io.File
import java.nio.file.Paths

object WACCCompiler {
    const val DEBUG = false

    @JvmStatic
    fun main(args: Array<String>) {
        val inputFilePath = args[0]
        val onlyCheck = "-OC" in args
        var inputStream: CharStream? = null

        try {
            inputStream = CharStreams.fromFileName(inputFilePath)
        } catch (e: IOException) {
            System.err.println("Could not read given file")
            if (DEBUG) {
                e.printStackTrace()
            }
            System.exit(-1)
        }

        val inputFileName = Paths.get(inputFilePath).fileName.toString()
        if (!inputFileName.endsWith(".wacc")) {
            System.err.println("Cannot compile a non-WACC file (.wacc)")
            System.exit(-1)
        }

        // Parse given program
        val lexer = WACCLexer(inputStream)
        val errorListener = WACCErrorListener()
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = WACCParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        val tree = parser.program()
        if (DEBUG) {
            println(tree.toStringTree(parser))
        }

        // Perform semantic analysis
        val symbolTable = WACCSymbolTable(null)
        val analyser = WACCSemanticAnalyser(symbolTable, errorListener)
        val analysedTree = analyser.visit(tree) as WACCProgram

        if (!onlyCheck) {
            // Perform the compilation and save to corresponding .s file
            val codeGenerator = WACCCodeGenerator(symbolTable)
            val instructions = codeGenerator.translate(analysedTree)
            val outputText = instructions.joinToString(separator = "\n") + '\n'
            val outputFileName = inputFileName.replace(".wacc", ".s")
            File(outputFileName).writeText(outputText)
        }
    }
}
