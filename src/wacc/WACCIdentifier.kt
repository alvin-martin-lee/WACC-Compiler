package wacc

/* Size constants in bytes. */
const val SIZE_BYTE = 1
const val SIZE_INT = 4
const val SIZE_PTR = 4
const val SIZE_UNDEFINED = -1
/**
 * WACC types
 *
 * size refers to the size a variable of this type would occupy on the stack.
 * For example the size of an array is the size of the pointer (i.e. 4 bytes)
 * not the number of elements.
 */
enum class WACCType(val printable: Boolean, val size: Int) {
    INT(true, SIZE_INT),
    BOOL(true, SIZE_BYTE),
    CHAR(true, SIZE_BYTE),
    PAIR(true, SIZE_PTR),
    ARRAY(true, SIZE_PTR),
    ARGS(true, SIZE_UNDEFINED),
    FUNC(false, SIZE_UNDEFINED),
    STAT(false, SIZE_UNDEFINED),
    PROGRAM(false, SIZE_UNDEFINED),
    NONE(true, SIZE_UNDEFINED);
}

// BASE TYPES

open class WACCIdentifier(private val type: WACCType, private var stackOffset: Int = 0) {
    open fun getType(): WACCType {
        return type
    }

    open fun getBaseType(): WACCIdentifier {
        return this
    }

    open fun getStackOffset(): Int {
        return stackOffset
    }

    open fun setStackOffset(offset: Int) {
        stackOffset = offset
    }
}

open class WACCInt : WACCIdentifier(WACCType.INT) {
    final override fun getBaseType(): WACCIdentifier {
        return this
    }
}

open class WACCBool : WACCIdentifier(WACCType.BOOL) {
    final override fun getBaseType(): WACCIdentifier {
        return this
    }
}

open class WACCChar : WACCIdentifier(WACCType.CHAR) {
    final override fun getBaseType(): WACCIdentifier {
        return this
    }
}

open class WACCArray(private val arrayType: WACCType, private var arrayCellBaseType: WACCType? = null): WACCIdentifier(WACCType.ARRAY) {
    fun getArrayType(): WACCType {
        return arrayType
    }

    fun setArrayCellBaseType(type: WACCType) {
        arrayCellBaseType = type
    }

    fun getArrayCellBaseType(): WACCType? {
        return arrayCellBaseType
    }

    final override fun getBaseType(): WACCIdentifier {
        return this
    }
}

open class WACCPair(private val value1: WACCIdentifier,
                    private val value2: WACCIdentifier) : WACCIdentifier(WACCType.PAIR) {
    fun getValue1(): WACCIdentifier {
        return value1
    }

    fun getValue2(): WACCIdentifier {
        return value2
    }

    override fun getBaseType(): WACCIdentifier {
        return this
    }
}

class WACCIdent(type: WACCType, private val ident: String) : WACCIdentifier(type) {
    override fun toString(): String {
        return ident
    }
}


// WACC LITERAL CLASSES - STORES LITERAL VALUES


class WACCBoolLiter(private val value: Boolean) : WACCBool() {
    fun getValue(): Boolean {
        return value
    }
}

class WACCIntLiter(private val value: Long) : WACCInt() {
    fun getValue(): Long {
        return value
    }
}

class WACCCharLiter(private val value: Char) : WACCChar() {
    fun getValue(): Char {
        return value
    }
}

open class WACCString : WACCArray(WACCType.CHAR, WACCType.CHAR) {
    override fun getType(): WACCType {
        return WACCType.ARRAY
    }
}

class WACCStringLiter(private val value: String) : WACCString(), WACCExpression {
    fun getValue(): String {
        return value
    }
}

class WACCPairLiter : WACCPair(WACCIdentifier(WACCType.NONE), WACCIdentifier(WACCType.NONE))

class WACCParam(private val ident: WACCIdentifier) : WACCIdentifier(ident.getType()) {
    fun getIdent(): WACCIdentifier {
        return ident
    }
}

class WACCArrayLit(private val arrayElems: MutableList<WACCIdentifier>,
                   private var elemType: WACCType) : WACCArray(elemType) {
    fun getArrayElems(): List<WACCIdentifier> {
        return arrayElems
    }

    fun getElemType(): WACCType {
        return elemType
    }

    fun setElemType(newElemType: WACCType) {
        elemType = newElemType
    }
}


// PROGRAM TYPE - Root node of the tree


class WACCProgram(private val funcs: List<WACCFunction>, private val stat: WACCStatement) : WACCIdentifier(WACCType.PROGRAM) {
    fun getFuncs(): List<WACCFunction> {
        return funcs
    }

    fun getStat(): WACCStatement {
        return stat
    }
}


// FUNCTION TYPES


class WACCArgList(private val argTypes: List<WACCIdentifier>) : WACCIdentifier(WACCType.ARGS) {
    fun getArgTypes(): List<WACCIdentifier> {
        return argTypes
    }
}

class WACCFunction(private val name: String,
                   private val returnType: WACCIdentifier,
                   private val parameters: List<WACCParam>,
                   private val symbolTable: WACCSymbolTable,
                   private var rootStatement: WACCStatement? = null) : WACCIdentifier(WACCType.FUNC) {

    fun setRootStatement(newStat: WACCStatement) {
        rootStatement = newStat
    }

    fun getName(): String {
        return name
    }

    fun getReturnType(): WACCIdentifier {
        return returnType
    }

    fun getSymbolTable(): WACCSymbolTable {
        return symbolTable
    }

    fun getParameters(): List<WACCParam> {
        return parameters
    }

    fun getRootStatement(): WACCStatement? {
        return rootStatement
    }
}


// STATEMENT AND EXPRESSION TYPES

//TODO refactor out into seperate child classes e.g. loops
open class WACCStatement(private val statType: StatTypes,
                    private val lhs: WACCIdentifier? = null,
                    private val rhs: WACCIdentifier? = null,
                    private val expr: WACCIdentifier? = null,
                    private val childStats: MutableList<WACCStatement>? = null,
                    private val symTab: WACCSymbolTable,
                    private val containsBreak: Boolean = false) : WACCIdentifier(WACCType.STAT) {
    enum class StatTypes {
        SKIP, ASSIGN, READ, FREE, RETURN, EXIT, PRINT, PRINTLN, IF, WHILE, STAT_BLOCK, STAT_MULT, FOR, DO_WHILE, BREAK, PROCEED, SWITCH,
        STRLEN, STRLOWER, STRUPPER
    }

    fun getStatType(): StatTypes {
        return statType
    }

    fun getLhs(): WACCIdentifier? {
        return lhs
    }

    fun getRhs(): WACCIdentifier? {
        return rhs
    }

    fun getExpr(): WACCIdentifier? {
        return expr
    }

    fun getSymbolTable(): WACCSymbolTable {
        return symTab
    }

    fun getChildStats(): MutableList<WACCStatement>? {
        return childStats
    }

    fun getContainsBreak(): Boolean {
        return containsBreak
    }
}

//TODO come up with final WACCIdentifier type for this
class WACCSwitchCase(private val case: WACCIdentifier, private val stat: WACCStatement): WACCIdentifier(WACCType.NONE) {
    fun getCase(): WACCIdentifier {
        return case
    }

    fun getStat(): WACCStatement {
        return stat
    }
}

class WACCSwitch(private val ident: WACCIdentifier,
                 private val cases: MutableList<WACCSwitchCase>,
                 symTab: WACCSymbolTable) : WACCStatement(StatTypes.SWITCH, symTab = symTab) {

    fun getIdent(): WACCIdentifier {
        return ident
    }

    fun getCases(): MutableList<WACCSwitchCase> {
        return cases
    }
}

interface WACCExpression

class WACCFunctionCall(type: WACCType, private val funcName: String, private val args: WACCArgList) : WACCIdentifier(type) {
    fun getFuncName(): String {
        return funcName
    }

    fun getArgs(): WACCArgList {
        return args
    }
}

class WACCMathSTDFuncCall(private val func: Int, private val args: MutableList<WACCIdentifier>): WACCIdentifier(WACCType.INT) {

    fun getFuncEnum(): Int {
        return func
    }

    fun getArgs(): MutableList<WACCIdentifier> {
        return args
    }
}

class WACCBinOperExpr(type: WACCType, private val lhs: WACCIdentifier, private val rhs: WACCIdentifier, private val operator: Int): WACCIdentifier(type), WACCExpression {
    fun getOper(): Int {
        return operator
    }

    fun getLhs(): WACCIdentifier {
        return lhs
    }

    fun getRhs(): WACCIdentifier {
        return rhs
    }

}

class WACCUnOperExpr(type: WACCType, private val lhs: WACCIdentifier,  private val operator: Int): WACCIdentifier(type), WACCExpression {
    fun getOper(): Int {
        return operator
    }

    fun getLhs(): WACCIdentifier {
        return lhs
    }
}

class WACCArrayElem(private val arrayIdent: WACCIdent,
                    private val indices: List<WACCIdentifier>,
                    private val arrayType: WACCType): WACCIdentifier(arrayType) {
    fun getArrayIdent(): WACCIdent {
        return arrayIdent
    }

    fun getIndices(): List<WACCIdentifier> {
        return indices
    }

    fun getArrayType(): WACCType {
        return arrayType
    }
}
