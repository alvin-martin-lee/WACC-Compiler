package wacc

import com.google.common.collect.LinkedHashMultimap

/**
 * Symbol Table class which stores the scope(s) of a WACC program.
 */
class WACCSymbolTable(internal var enclosingSymbolTable: WACCSymbolTable?) {
    private val dict: LinkedHashMultimap<String, WACCIdentifier> = LinkedHashMultimap.create()
    var childSymbolTables: MutableList<WACCSymbolTable> = mutableListOf()

    init {
        enclosingSymbolTable?.addChildSymTab(this)
    }

    /**
     * Maps a ident/variable to an element.
     */
    fun add(name: String, obj: WACCIdentifier) {
        dict.put(name, obj)
    }

    /**
     * Retrieves a WACC element from an id name within this symbol table
     */
    fun lookup(name: String, index: Int = 0): WACCIdentifier? {
        return dict.get(name).elementAtOrNull(index)
    }

    /**
     * Looks up a WACC ID from the current scope and its outer scopes (if any).
     *
     * If a WACC element is found, this is returned and no further tables are searched.
     */
    fun lookupAll(name: String, index: Int = 0): WACCIdentifier? {
        var symbolTable: WACCSymbolTable? = this
        while (symbolTable != null) {
            val obj = symbolTable.lookup(name, index)
            if (obj != null) {
                return obj
            }
            symbolTable = symbolTable.enclosingSymbolTable
        }
        return null
    }

    /**
     * Add a child symbol table to this symbol table.
     */
    fun addChildSymTab(childSymbolTable: WACCSymbolTable) {
        childSymbolTables.add(childSymbolTable)
    }

    /**
     * Retrieves the root symbol table
     */
    fun getTopLevelSymbolTable(): WACCSymbolTable {
        var current: WACCSymbolTable = this
        while (current.enclosingSymbolTable != null) {
            current = current.enclosingSymbolTable!!
        }
        return current
    }

    /**
     * Calculate the total size of local variables in bytes that would occupy the stack.
     */
    fun scopeStackSize(): Int {
        var size = 0
        for (value in dict.values()) {
            if (value !is WACCFunction && value !is WACCParam) {
                val entrySize: Int = value.getType().size
                assert(entrySize != SIZE_UNDEFINED)
                size += entrySize
            }
        }
        return size
    }

    /**
     * Calculate offset of a local variable relative to the current scope.
     *
     * The variable could live in an outer scope (multiple layers).
     */
    fun calculateStackOffset(name: String, type: WACCType = WACCType.NONE): Int {
        assert(lookupAll(name) != null)

        // Overall offset of the scope that x lives in to the current scope
        var scope: WACCSymbolTable = this
        var scopesOffset = 0

        // Checks if defined in the enclosing table as well
        if (enclosingSymbolTable != null) {
            val isDefinedInEncSymTab = scope.enclosingSymbolTable!!.lookupAll(name) != null
            var isInitInCurScope = false

            val localVariable = lookup(name)
            if (localVariable != null) {
                isInitInCurScope = type == localVariable.getType()
            }

            if (isDefinedInEncSymTab && !isInitInCurScope && type != WACCType.NONE) {
                scopesOffset += scope.scopeStackSize()
            }
        }

        while (scope.lookup(name) == null) {
            scopesOffset += scope.scopeStackSize()
            scope = scope.enclosingSymbolTable!!
        }

        // Offset of x relative to the scope it lives in
        val entry = scope.lookup(name)!!
        assert (entry !is WACCFunction && entry !is WACCParam) // variable only
        val localOffset = entry.getStackOffset()
        return scopesOffset + localOffset
    }


    /**
     * Initialize all the stack offsets of local variables in the current scope.
     *
     * Call this in the code generation stage every time the translate function
     * enters a new scope. This sets the offsets of all local variables within
     * the current scope relative to the current stack pointer.
     *
     * Example of how code generation uses stack offsets:
     * begin
     *   # sp = sp - 13 (allocate 13 bytes of stack space for all local variables)
     *   int x = 1234;                     # sp + 9
     *   int y = 5678;                     # sp + 5
     *   char z = 'z';                     # sp + 4
     *   pair(int, int) p = newpair(1, 2); # sp
     *   <more code...>
     *   # sp = sp + 13 (pop local vars off the stack)
     * end
     */
    fun initializeLocalVariableStackOffsets() {
        val scopeSize = scopeStackSize()
        var acc = 0

        var accParam = 4

        if (scopeSize > 0) {
            accParam += 4
        }

        var isFirstArg = true

        for ((_, entry) in dict.entries()) {
            // local variables
            if (entry !is WACCFunction && entry !is WACCParam) {
                val size = entry.getType().size
                acc += size
                entry.setStackOffset(scopeSize - acc)
            }
            if (entry is WACCParam) {
                if (!isFirstArg) {
                    val size = entry.getType().size
                    accParam += size
                } else {
                    isFirstArg = false
                }
                entry.setStackOffset(accParam)
            }
        }
    }

    /**
     * Print current scope for debugging.
     */
    fun printContents() {
        if (enclosingSymbolTable == null) {
            println("Top Level Symbol Table")
        }
        println("Current scope values:")
        for ((key, value) in dict.entries()) {
            println(" - $key: ${value.getType()}")
        }
        println("Child scopes: ${childSymbolTables.size}")
    }
}
