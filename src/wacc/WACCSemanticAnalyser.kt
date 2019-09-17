package wacc

import antlr.WACCLexer
import antlr.WACCParser
import antlr.WACCParserBaseVisitor
import org.antlr.v4.runtime.RuleContext

class WACCSemanticAnalyser(private var symbolTable: WACCSymbolTable,
                           private var errorHandler: WACCErrorListener)
    : WACCParserBaseVisitor<WACCIdentifier>() {

    // Loop variables
    private var isInsideLoop = false
    private var isInsideSwitch = false
    private var containsBreak = false

    // Map of full function names (including return and param types) to name stored in symbol table
    private var funcNameList = mutableListOf<String>()

    /***********************
     * Program & Functions *
     ***********************/

    /**
     * Add function definitions to the symbol table.
     *
     * This is called in visitProg before the function bodies are checked.
     */
    private fun initFunctionDefinitions(funcCtxs: List<WACCParser.FuncContext>) {

        for (funcCtx: WACCParser.FuncContext in funcCtxs) {
            val name: String = funcCtx.ident().text

            val returnType = visit(funcCtx.type())
            val parameters = mutableListOf<WACCParam>()
            val scope = WACCSymbolTable(symbolTable)

            // Initialize parameters and add them to function-specific scope
            val paramListCtx = funcCtx.paramList()
            if (paramListCtx != null) {
                for (paramCtx in paramListCtx.param()) {
                    val param = visit(paramCtx) as WACCParam
                    scope.add(paramCtx.ident().text, param)
                    parameters.add(param)
                }
            }
            val paramIterator = parameters.iterator()
            var paramTypes = ""
            if (paramIterator.hasNext()) {
                for (param in paramIterator) {
                    paramTypes = paramTypes.plus("_" + param.getIdent().getType().toString())
                }
            } else {
                paramTypes = "_NONE"
            }
            val fullFuncName = funcCtx.ident().text + "_returns_" + returnType.getType().toString() + "_params" + paramTypes
            if (funcNameList.contains(fullFuncName)) {
                errorHandler.visitorError(WACCErrorType.SEMANTIC,
                        "\"$name\" is already defined in this scope", funcCtx)
            }
            funcNameList.add(fullFuncName)
            symbolTable.add(name, WACCFunction(name, returnType, parameters, scope))
        }
    }

    /**
     * Entry point of the semantic analyser.
     * Checks the function definitions then the main body.
     */
    override fun visitProgram(ctx: WACCParser.ProgramContext): WACCIdentifier {
        // Analyses and gathers the details of the program's functions
        val functionContexts = ctx.func()
        initFunctionDefinitions(functionContexts)
        val funcs = ArrayList<WACCFunction>(functionContexts.size)
        functionContexts.forEach { funcs.add(visit(it) as WACCFunction) }

        // Analyses and gathers the details of the program's statements
        val rootStat = visit(ctx.stat()) as WACCStatement

        // Generates the root program node
        val program = WACCProgram(funcs, rootStat)
        return program
    }

    /**
     * Check function definition
     *
     * This assumes all function definitions are initialized into the symbol table
     * and that for each table its parameters are in the function scope.
     */
    override fun visitFunc(ctx: WACCParser.FuncContext): WACCIdentifier {
        // Check function body in its own scope
        val enclosingSymbolTable = symbolTable
        val entry = symbolTable.lookupAll(ctx.ident().text) as WACCFunction

        symbolTable = entry.getSymbolTable()

        if (!funcBodyReturnsOrExits(ctx.stat())) {
            return errorHandler.visitorError(WACCErrorType.SYNTAX,
                    "Function: ${ctx.ident().text} does not return or exit", ctx)
        }

        // Important: visit function body with symbol table set to the function's inner scope
        entry.setRootStatement(visit(ctx.stat()) as WACCStatement)

        // Restore previous scope
        symbolTable = enclosingSymbolTable
        return entry
    }

    /**
     * Recursively check that all execution paths of a function body (stat) returns or exits.     *
     *
     * Provide the function's body (i.e. StatContext) and its expected return type as arguments.
     * Note that this does not check the returned expression or exit code.
     */
    private fun funcBodyReturnsOrExits(ctx: WACCParser.StatContext): Boolean {
        if (ctx.EXIT() != null && ctx.expr() != null) {
            return true
        }

        if (ctx.RETURN() != null && ctx.expr() != null) {
            return true
        }

        if (ctx.IF() != null && ctx.thenBody != null && ctx.elseBody != null) {
            return funcBodyReturnsOrExits(ctx.thenBody) &&
                    funcBodyReturnsOrExits(ctx.elseBody)
        }

        if (ctx.currentStat != null && ctx.SEMICOLON() != null && ctx.nextStat != null) {
            return funcBodyReturnsOrExits(ctx.nextStat)
        }

        if (ctx.BEGIN() != null && ctx.END() != null) {
            return funcBodyReturnsOrExits(ctx.stat(0))
        }

        // Statements that don't return or exit: skip, assignments, read, free, print/ln, while loop
        // According to the reference compiler, even if a function contains a while loop that returns
        // something it is still an error. e.g. int f() is while true do return 1 done end
        return false

    }

    /**
     * Single function parameter (i.e. type and identifier).
     */
    override fun visitParam(ctx: WACCParser.ParamContext): WACCIdentifier {
        val paramType = visit(ctx.type())
        return WACCParam(paramType)
    }

    /**************
     * Statements *
     **************/

    /**
     * Visits a statement and checks the statement for errors.
     */
    override fun visitStat(ctx: WACCParser.StatContext): WACCIdentifier {
        when {
            // Process SKIP statements
            ctx.SKIP_() != null -> return WACCStatement(WACCStatement.StatTypes.SKIP, symTab = symbolTable)

            // Process BREAK statements
            ctx.BREAK() != null -> {
                if (!isInsideLoop && !isInsideSwitch) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "BREAK statements must be within a loop", ctx)
                } else {
                    containsBreak = true
                    return WACCStatement(WACCStatement.StatTypes.BREAK, symTab = symbolTable)
                }
            }

            // Process PROCEED statements
            ctx.PROCEED() != null -> {
                if (!isInsideLoop) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "PROCEED statements must be within a loop", ctx)
                } else {
                    return WACCStatement(WACCStatement.StatTypes.PROCEED, symTab = symbolTable)
                }
            }

            // Process assignment statement when a variable is declared and assigned immediately
            ctx.type() != null -> {
                val type = visit(ctx.type())
                val ident = ctx.ident().text
                val rhs = visit(ctx.assignRhs())

                // Type checks lhs and rhs
                if (type.getType() != rhs.getType()) {
                    if (rhs.getType() == WACCType.NONE) {
                        // For backend to generate run time error
                    } else {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Expected type ${type.getType()} found ${rhs.getType()}", ctx)
                    }
                }

                // Sets the type of empty array literals to 'type'
                if (rhs is WACCArrayLit && type is WACCArray) {
                    if (rhs.getElemType() == WACCType.NONE) {
                        rhs.setElemType(type.getArrayType())
                    }
                    rhs.setArrayCellBaseType(type.getArrayCellBaseType()!!)
                }

                // Sets the type of the variable to 'type'
                var elemToStore = rhs

                // Accounts for potentially initially empty pairs/arrays e.g. []
                if (rhs.getType() == WACCType.PAIR && rhs !is WACCPair) {
                    elemToStore = type
                }

                if (rhs.getType() == WACCType.ARRAY && rhs !is WACCArray) {
                    elemToStore = type
                }

                // If the rhs is a variable, we check if it has been declared
                if (rhs is WACCIdent) {
                    elemToStore = symbolTable.lookupAll(rhs.toString())

                    if (elemToStore == null) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Could not assign $rhs to $ident " +
                                        "as the $rhs has not been declared", ctx)
                    }
                }

                // Check if new variable has already been declared
                val entry = symbolTable.lookup(ident.toString())

                if (entry != null && entry !is WACCFunction) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "$ident has already been declared", ctx)
                }

                // Add variable entry to the symbol table
                symbolTable.add(ident.toString(), elemToStore)

                return WACCStatement(WACCStatement.StatTypes.ASSIGN, WACCIdent(type.getType(), ident), rhs, symTab = symbolTable)
            }

            // Process assignment statements for variables reassignments
            ctx.assignLhs() != null && ctx.assignRhs() != null -> {
                val lhs = visit(ctx.assignLhs())
                val rhs = visit(ctx.assignRhs())

                // Treats strings as a character array
                if (lhs.getType() == WACCType.ARRAY && lhs is WACCString) {
                    if (rhs.getType() != WACCType.CHAR && rhs.getType() != WACCType.ARRAY) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "String assignments type invalid" +
                                        " (expected: STRING or CHAR, actual: ${rhs.getType()}", ctx)
                    }
                } else if (lhs is WACCIdent) {
                    // NOTE: lhs could be an array
                    // Ensures that the variable has been declared before assignment
                    val lhsElem = symbolTable.lookupAll(lhs.toString())
                            ?: return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                    "$lhs has not been declared", ctx)

                    // Ensures that the lhs element type is the same as the new value
                    if (lhsElem is WACCArrayLit) {
                        // i.e. rhs should be an array
                        if (rhs.getType() != WACCType.ARRAY) {
                            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                    "Expected type ARRAY " +
                                            "found ${rhs.getType()}", ctx)
                        }
                        // inspect element type
                        if (rhs is WACCArrayLit) {
                            if (lhsElem.getElemType() != rhs.getElemType()) {
                                return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                        "Expected type ${lhsElem.getElemType()} " +
                                                "found ${rhs.getType()}", ctx)
                            } else {
                                val rhsElem = symbolTable.lookupAll(lhs.toString())
                                        ?: return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                                "$rhs has not been declared", ctx)
                                if (rhsElem !is WACCArrayLit) {
                                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                            "Expected type ARRAY found ${rhsElem.getType()}", ctx)
                                }
                                if (lhsElem.getElemType() != rhs.getElemType()) {
                                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                            "Expected type ${lhsElem.getElemType()} " +
                                                    "found ${rhsElem.getElemType()}", ctx)
                                }
                            }

                        }
                    } else if (lhsElem.getType() != rhs.getType()) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Expected type ${lhs.getType()} " +
                                        "found ${rhs.getType()}", ctx)
                    }
                }

                return WACCStatement(WACCStatement.StatTypes.ASSIGN, lhs, rhs, symTab = symbolTable)
            }

            // Process READ statements
            ctx.READ() != null -> {
                val lhs = visit(ctx.assignLhs())

                // Ensures that lhs is valid
                if (lhs != null) {
                    // Case where lhs is a variable
                    if (lhs is WACCIdent) {
                        val ident: WACCIdent = lhs
                        val objAtIdent = symbolTable.lookupAll(ident.toString())

                        // Ensures that the variable has be declared/exists
                        if (objAtIdent == null) {
                            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                    "$ident has not been declared", ctx)
                        } else {
                            // Readable types must have a base of CHAR or INT
                            if (objAtIdent.getType() != WACCType.CHAR &&
                                    objAtIdent.getType() != WACCType.INT) {

                                return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                        "Incompatible type ${objAtIdent.getType()}", ctx)
                            }
                        }
                    } else if (lhs.getType() != WACCType.CHAR &&
                            lhs.getType() != WACCType.INT &&
                            lhs.getType() != WACCType.NONE) {
                        // NONE is an acceptable types for now and will generate a runtime error
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Incompatible type ${lhs.getType()}", ctx)
                    }
                } else {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "READ Statement missing lhs", ctx)
                }

                return WACCStatement(WACCStatement.StatTypes.READ, lhs = lhs, symTab = symbolTable)
            }

            // Process FREE statements
            ctx.FREE() != null -> {
                val expr = visit(ctx.expr())
                val ident = symbolTable.lookupAll(expr.toString())
                        ?: return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "$expr is not accessible in the current scope", ctx)
                val type = ident.getType()

                // Ensures that FREE can only apply to PAIR and ARRAYs
                if (type != WACCType.PAIR && type != WACCType.ARRAY) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Trying to free a non pair or array", ctx)
                }

                return WACCStatement(WACCStatement.StatTypes.FREE, expr = expr, symTab = symbolTable)
            }

            // Process RETURN statements
            ctx.RETURN() != null -> {
                val returnExpr = visit(ctx.expr())

                // Check that the return statement is within a function
                // AND returns the expected type
                var currentCtx: RuleContext? = ctx
                loop@ while (currentCtx != null) {
                    currentCtx = currentCtx.getParent()
                    when (currentCtx) {
                        is WACCParser.FuncContext -> {
                            val funcIdent = currentCtx.ident().text
                            val topLevelScope = symbolTable.getTopLevelSymbolTable()
                            var index = 0
                            var func = topLevelScope.lookup(funcIdent, index) as WACCFunction?
                            functionLoop@ while (true) {
                                val expectedReturnType = func!!.getReturnType().getType()
                                val actualReturnType = returnExpr.getType()
                                if (expectedReturnType == returnExpr.getType()) {
                                    break@functionLoop
                                }
                                index++
                                func = topLevelScope.lookup(funcIdent, index) as WACCFunction?
                                if (func == null) {
                                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                            "Incompatible return type (expected: $expectedReturnType, " +
                                                    "actual: $actualReturnType)", ctx)
                                }
                            }
                            break@loop
                        }
                        is WACCParser.ProgramContext -> {
                            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                    "Cannot return from the global scope.", ctx)
                        }
                    }
                }

                return WACCStatement(WACCStatement.StatTypes.RETURN, expr = returnExpr, symTab = symbolTable)

            }

            // Process EXIT statements
            ctx.EXIT() != null -> {
                val expr = visit(ctx.expr())

                if (expr.getType() != WACCType.INT) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Exit statements must have integer exit code", ctx)
                }

                return WACCStatement(WACCStatement.StatTypes.EXIT, expr = expr, symTab = symbolTable)
            }

            // Process PRINT statements
            ctx.PRINT() != null -> {
                val expr = visit(ctx.expr())

                if (!expr.getType().printable) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "${expr.getType()} is not a printable type", ctx)
                }

                return WACCStatement(WACCStatement.StatTypes.PRINT, expr = expr, symTab = symbolTable)
            }

            // Process PRINTLN statements
            ctx.PRINTLN() != null -> {
                val expr = visit(ctx.expr())

                if (!expr.getType().printable) {
                    val ident = expr.toString()
                    var index = 0
                    var entry = symbolTable.lookupAll(ident, index)
                    while (entry != null) {
                        if (entry.getType().printable) {
                            return WACCStatement(WACCStatement.StatTypes.PRINTLN, expr = entry, symTab = symbolTable)
                        }
                        index++
                        entry = symbolTable.lookupAll(ident, index)
                    }
                    //Semantic Error
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "${expr.getType()} is not printable", ctx)
                }

                return WACCStatement(WACCStatement.StatTypes.PRINTLN, expr = expr, symTab = symbolTable)
            }

            // Process IF statements
            ctx.IF() != null -> {
                val predicate = visit(ctx.expr())
                val statements: MutableList<WACCStatement> = mutableListOf()

                // Ensures predicate is a BOOL
                if (predicate.getType() != WACCType.BOOL) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "IF predicate is not a bool (expected: BOOL, " +
                                    "actual: ${predicate.getType()}", ctx)
                }

                // Processes the two statement blocks in their own scope
                for (i in ctx.stat().indices) {
                    val newSymTab = WACCSymbolTable(symbolTable)
                    symbolTable = newSymTab

                    statements.add(visit(ctx.stat(i)) as WACCStatement)

                    symbolTable = symbolTable.enclosingSymbolTable!!
                }

                return WACCStatement(WACCStatement.StatTypes.IF,
                        expr = predicate, childStats = statements, symTab = symbolTable)
            }

            // Process WHILE & DO WHILE statements
            ctx.WHILE() != null -> {
                var whileLoopType = WACCStatement.StatTypes.WHILE

                if (ctx.start.type == WACCLexer.DO) {
                    whileLoopType = WACCStatement.StatTypes.DO_WHILE
                }

                val predicate = visit(ctx.expr())

                // Ensures predicate is a BOOL
                if (predicate.getType() != WACCType.BOOL) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "IF predicate is not a bool", ctx)
                }

                isInsideLoop = true

                // Processes the WHILE's body in its own scope
                val newSymTab = WACCSymbolTable(symbolTable)
                symbolTable = newSymTab

                val childStat: WACCStatement = visit(ctx.stat(0)) as WACCStatement

                symbolTable = symbolTable.enclosingSymbolTable!!

                isInsideLoop = false

                val whileStat = WACCStatement(whileLoopType, expr = predicate,
                        childStats = mutableListOf(childStat), symTab = symbolTable, containsBreak = containsBreak)

                containsBreak = false

                return whileStat
            }

            // Process FOR statements
            ctx.FOR() != null -> {
                // Ensures initialisation of loop is INT TODO extend for other types
                val setupStat = visit(ctx.stat(0)) as WACCStatement
                var setupStatType = WACCType.NONE

                if (setupStat.getStatType() != WACCStatement.StatTypes.ASSIGN) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "FOR intialisation statement must be a declaration of a INT", ctx)
                } else {
                    setupStatType = setupStat.getLhs()!!.getType()
                    if (setupStatType != WACCType.INT) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "FOR initialisation statement must be of type INT found $setupStatType", ctx)
                    }
                }

                // Ensures condition stat is a bool
                val conditionExpr = visit(ctx.expr())
                if (conditionExpr.getType() != WACCType.BOOL) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "FOR condition is not a bool", ctx)
                }

                // Ensures increment of loop is INT TODO extend for other types
                val incrementStat = visit(ctx.stat(1)) as WACCStatement
                if (incrementStat.getStatType() != WACCStatement.StatTypes.ASSIGN) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "FOR increment statement must be a assignment statement of type int", ctx)
                } else {
                    val incStatType = incrementStat.getLhs()!!.getType()
                    if (incStatType != setupStatType || incStatType == WACCType.NONE) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "FOR increment statement must be of the type $setupStatType found $incStatType", ctx)
                    }
                }

                isInsideLoop = true

                // Processes the FOR's body in its own scope
                val newSymTab = WACCSymbolTable(symbolTable)
                symbolTable = newSymTab

                val childStat: WACCStatement = visit(ctx.stat(2)) as WACCStatement

                symbolTable = symbolTable.enclosingSymbolTable!!

                isInsideLoop = false

                val forStat = WACCStatement(
                        WACCStatement.StatTypes.FOR, expr = conditionExpr, lhs = setupStat, rhs = incrementStat,
                        childStats = mutableListOf(childStat), symTab = symbolTable, containsBreak = containsBreak)

                containsBreak = false

                return forStat
            }

            // Process SWITCH statements
            //     | SWITCH ident WHERE (switchCase)+ DONE
            ctx.SWITCH() != null -> {
                val ident = visit(ctx.ident())

                val cases = mutableListOf<WACCSwitchCase>()

                isInsideSwitch = true

                for (switchCase in ctx.switchCase()) {
                    cases.add(visit(switchCase) as WACCSwitchCase)
                }

                isInsideSwitch = false

                return WACCSwitch(ident, cases, symbolTable)
            }

            // Process BEGIN statements
            ctx.BEGIN() != null -> {
                // Process the code block in its own scope
                val newSymTab = WACCSymbolTable(symbolTable)
                symbolTable = newSymTab

                val childStat: WACCStatement = visit(ctx.stat(0)) as WACCStatement

                symbolTable = symbolTable.enclosingSymbolTable!!

                return WACCStatement(WACCStatement.StatTypes.STAT_BLOCK,
                        childStats = mutableListOf(childStat), symTab = symbolTable)
            }

            // Process codeblocks with multiple statements
            ctx.SEMICOLON() != null -> {
                val stat1: WACCStatement = visit(ctx.stat(0)) as WACCStatement
                val stat2: WACCStatement = visit(ctx.stat(1)) as WACCStatement

                return WACCStatement(WACCStatement.StatTypes.STAT_MULT,
                        childStats = mutableListOf(stat1, stat2), symTab = symbolTable)
            }

            // Process string library functions
            ctx.STRLEN() != null -> TODO()
            ctx.STRLOWER() != null -> TODO()
            ctx.STRUPPER() != null -> TODO()
        }

        return WACCStatement(WACCStatement.StatTypes.STAT_MULT, symTab = symbolTable)
    }

    override fun visitSwitchCase(ctx: WACCParser.SwitchCaseContext): WACCIdentifier {
        val expr = visit(ctx.expr())
        val stat = visit(ctx.stat()) as WACCStatement

        return WACCSwitchCase(expr, stat)
    }

    /**
     * Check left-hand side of assignment statement.
     */
    override fun visitAssignLhs(ctx: WACCParser.AssignLhsContext): WACCIdentifier {
        return when {
            ctx.ident() != null -> {
                val ident = visit(ctx.ident())
                // Checks that the variable trying to be assigned to is not a function
                if (ident.getType() == WACCType.FUNC)
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Variable \"${ctx.ident().text}\" is not defined in this scope", ctx)

                ident
            }
            ctx.arrayElem() != null -> visit(ctx.arrayElem())
            ctx.pairElem() != null -> visit(ctx.pairElem())
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX,
                    "Unrecognised assignLhs format", ctx)
        }
    }

    /**
     * Visit right hand side of assignment statement.
     */
    override fun visitAssignRhs(ctx: WACCParser.AssignRhsContext): WACCIdentifier {
        when {
            ctx.rhsExpr != null -> return visit(ctx.rhsExpr)
            ctx.arrayLiter() != null -> return visit(ctx.arrayLiter())
            ctx.NEWPAIR() != null -> return makeNewPair(ctx.expr())
            ctx.pairElem() != null -> return visit(ctx.pairElem())
            ctx.CALL() != null -> {
                // Process function calls
                val funcIdent = ctx.ident().text
                val func = symbolTable.lookupAll(funcIdent) as? WACCFunction
                        ?: return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Function $funcIdent not found", ctx)

                var argList = WACCArgList(mutableListOf())
                if (ctx.argList() != null) {
                    argList = visit(ctx.argList()) as WACCArgList
                }
                return WACCFunctionCall(func.getReturnType().getType(), funcIdent, argList)
            }

        }

        return errorHandler.visitorError(WACCErrorType.SYNTAX, "Invalid assignRhs format", ctx)
    }


    /**
     * Visit a list of arguments in a function call.
     *
     * Checks that the arguments match the function's parameters.
     */
    override fun visitArgList(ctx: WACCParser.ArgListContext): WACCIdentifier {
        val callContext: WACCParser.AssignRhsContext = ctx.getParent() as WACCParser.AssignRhsContext
        val funcIdentCtx: WACCParser.IdentContext = callContext.ident()
        val functionObj: WACCFunction = symbolTable.lookupAll(funcIdentCtx.text) as WACCFunction
        val arguments: List<WACCParser.ExprContext> = ctx.expr()
        val parameters: List<WACCParam> = functionObj.getParameters()

        // Check same number of elements
        if (arguments.size != parameters.size) {
            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                    "Incorrect number of parameters for \"${funcIdentCtx.text}\" " +
                            "(expected: ${parameters.size}, actual: ${arguments.size})", ctx)
        }

        // Checks if parameter and arguement elements have the same type
        val args = ArrayList<WACCIdentifier>(arguments.size)

        for (i in arguments.indices) {
            val arg = visit(arguments[i])
            val argType = visit(arguments[i]).getType()
            val paramType: WACCType = parameters[i].getType()
            if (argType != paramType) {
                return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                        "Incompatible type at \"${arguments[i].text}\"" +
                                "(expected: $paramType, actual: $argType", ctx)
            }
            args.add(arg)
        }

        return WACCArgList(args)
    }

    /*****************
     * Types & Elems *
     *****************/

    /**
     * Check type definition
     *
     * This can be a base types, a pair or an array
     */
    override fun visitType(ctx: WACCParser.TypeContext): WACCIdentifier {
        if (ctx.type() != null) {
            val arrayType = visit(ctx.type())
            val baseType = getMultiDimensionArrayBaseType(ctx.type())
            return WACCArray(arrayType.getType(), baseType.getType())
        }
        return visit(ctx.getChild(0))
    }

    /** Get the base type of a multidimensional array. **/
    fun getMultiDimensionArrayBaseType(ctx: RuleContext): WACCIdentifier {
        //assert(ctx is WACCParser.TypeContext || ctx is WACCParser.BaseTypeContext)
        if (ctx is WACCParser.BaseTypeContext) {
            return visitBaseType(ctx)
        }
        if (ctx is WACCParser.PairTypeContext) {
            return visitPairType(ctx)
        }
        if (ctx is WACCParser.TypeContext) {
            if (ctx.type() != null) {
                return getMultiDimensionArrayBaseType(ctx.type())
            } else if (ctx.baseType() != null) {
                return getMultiDimensionArrayBaseType(ctx.baseType())
            } else if (ctx.pairType() != null) {
                return getMultiDimensionArrayBaseType(ctx.pairType())
            }
        }
        throw Exception("null type context. ${ctx.javaClass.name}")
    }

    /**
     * Check a base type (int, bool, char, string).
     */
    override fun visitBaseType(ctx: WACCParser.BaseTypeContext): WACCIdentifier {
        return when {
            ctx.INT() != null -> WACCIdentifier(WACCType.INT)
            ctx.BOOL() != null -> WACCIdentifier(WACCType.BOOL)
            ctx.CHAR() != null -> WACCIdentifier(WACCType.CHAR)
            ctx.STRING() != null -> WACCString()
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX, "Invalid base type", ctx)
        }
    }

    /**
     * Check pair type pair(type1, type2).
     */
    override fun visitPairType(ctx: WACCParser.PairTypeContext): WACCIdentifier {
        if (ctx.pairElemType().size != 2) {
            return errorHandler.visitorError(WACCErrorType.SYNTAX,
                    "Pair type must have 2 types specified, found ${ctx.pairElemType().size}", ctx)
        }

        // Analyses the two elements of the pair
        val fstType = visit(ctx.fst)
        val sndType = visit(ctx.snd)
        return WACCPair(fstType, sndType)
    }

    /**
     * Check pair element is valid (fst/snd p).
     */
    override fun visitPairElem(ctx: WACCParser.PairElemContext): WACCIdentifier {
        val pairIden = ctx.expr().text
        var pair = symbolTable.lookupAll(pairIden)

        if (pair is WACCParam && pair.getIdent() is WACCPair) {
            pair = pair.getIdent()
        }

        return when {
            pair == null -> errorHandler.visitorError(WACCErrorType.SEMANTIC,
                    "$pairIden is not declared", ctx)
            pair.getType() != WACCType.PAIR -> errorHandler.visitorError(WACCErrorType.SEMANTIC,
                    "$pairIden is not a valid Pair (expected: PAIR, actual: ${pair.getType()}", ctx)
            ctx.FST() != null && pair is WACCPair -> pair.getValue1()
            ctx.SND() != null && pair is WACCPair -> pair.getValue2()
            else -> return errorHandler.visitorError(WACCErrorType.SYNTAX,
                    "Invalid pairElem format", ctx)
        }
    }

    /**
     * Visit a pair element type inside a definition of a pair.
     */
    override fun visitPairElemType(ctx: WACCParser.PairElemTypeContext): WACCIdentifier {
        return when {
            ctx.baseType() != null -> visit(ctx.baseType())
            ctx.type() != null -> WACCIdentifier(WACCType.ARRAY)
            ctx.PAIR() != null -> WACCIdentifier(WACCType.PAIR)
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX, "Invalid pairElem type", ctx)
        }
    }

    /**
     * Check array element (e.g. a[0])
     *
     * Make sure array index is an integer and the array identifier is valid.
     */
    override fun visitArrayElem(ctx: WACCParser.ArrayElemContext): WACCIdentifier {
        // Ensures that the expression within the square brackets is an integer
        for (expression in ctx.expr()) {
            val expr = visit(expression)
            val exprType = expr.getType()
            if (exprType != WACCType.INT) {
                return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                        "Incompatible type at \"${expression.text}\" " +
                                "(expected: INT, actual: $exprType", ctx)
            }
        }

        // Retrieves data about the array being indexed
        val ident = visit(ctx.ident())
        var arrayElem = symbolTable.lookupAll(ident.toString())
                ?: return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                        "Identifier $ident used before declaration", ctx)

        if (arrayElem is WACCParam && arrayElem.getIdent() is WACCArray) {
            arrayElem = arrayElem.getIdent()
        }

        if (arrayElem !is WACCArray) {
            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                    "Identifier $ident is not an array", ctx)
        }

        // Save indices
        val indices = mutableListOf<WACCIdentifier>()
        for (expression in ctx.expr()) {
            indices.add(visit(expression))
        }
        return WACCArrayElem(ident as WACCIdent, indices, arrayElem.getArrayType())
    }

    /***************
     * Expressions *
     ***************/

    /**
     * Check expression.
     *
     * Delegates expression visiting to relevant analysis functions
     */
    override fun visitExpr(ctx: WACCParser.ExprContext): WACCIdentifier? {
        return when {
            ctx.uop != null -> visitUnaryOper(ctx.uop)
            ctx.intLiter() != null -> visitIntLiter(ctx.intLiter())
            ctx.boolLiter() != null -> visitBoolLiter(ctx.boolLiter())
            ctx.charLiter() != null -> visitCharLiter(ctx.charLiter())
            ctx.strLiter() != null -> visitStrLiter(ctx.strLiter())
            ctx.pairLiter() != null -> visitPairLiter(ctx.pairLiter())
            ctx.ident() != null -> visitIdent(ctx.ident())
            ctx.arrayElem() != null -> visitArrayElem(ctx.arrayElem())
            ctx.bop != null -> checkBinaryOperExpr(ctx)
            ctx.parenExpr != null -> visit(ctx.parenExpr)
            ctx.standardLibFunc() != null -> visit(ctx.standardLibFunc())
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX, "Invalid expression format", ctx)
        }
    }

    override fun visitStandardLibFunc(ctx: WACCParser.StandardLibFuncContext): WACCIdentifier {
        return visit(ctx.standardMathFunc())
    }

    override fun visitStandardMathFunc(ctx: WACCParser.StandardMathFuncContext): WACCIdentifier {
        val args = mutableListOf<WACCIdentifier>()

        for (expr in ctx.expr()) {
            val e = visit(expr)

            if (e.getType() != WACCType.INT) {
                errorHandler.visitorError(WACCErrorType.SYNTAX,
                        "Math Standard Library Functions only take integers as parameters, found ${e.getType()}", ctx)
            }

            args.add(e)
        }

        return WACCMathSTDFuncCall(ctx.start.type, args)
    }

    /**
     * Visit unary operator. Check that the operand expression is compatible.
     */
    override fun visitUnaryOper(ctx: WACCParser.UnaryOperContext): WACCIdentifier {
        val parentCtx: WACCParser.ExprContext = ctx.getParent() as WACCParser.ExprContext
        val assocExpr: WACCIdentifier = visit(parentCtx.expr(0))

        return when (ctx.start.type) {
            WACCParser.NOT -> {
                if (assocExpr.getType() != WACCType.BOOL) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "NOT operation only takes BOOLs " +
                                    "(expected: BOOL, actual: ${assocExpr.getType()})", ctx)
                }

                WACCUnOperExpr(WACCType.BOOL, assocExpr, ctx.start.type)
            }

            WACCParser.MINUS -> {
                if (assocExpr.getType() != WACCType.INT) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "MINUS operation only takes INT " +
                                    "(expected: INT, actual: ${assocExpr.getType()})", ctx)
                }
                WACCUnOperExpr(WACCType.INT, assocExpr, ctx.start.type)
            }

            WACCParser.LEN -> {
                if (assocExpr.getType() != WACCType.ARRAY) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "NOT operation only takes ARRAY " +
                                    "(expected: BOOL, actual: ${assocExpr.getType()})", ctx)
                }
                WACCUnOperExpr(WACCType.INT, assocExpr, ctx.start.type)
            }

            WACCParser.ORD -> {
                if (assocExpr.getType() != WACCType.CHAR) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "ORD operation only takes CHAR " +
                                    "(expected: CHAR, actual: ${assocExpr.getType()})", ctx)
                }
                WACCUnOperExpr(WACCType.INT, assocExpr, ctx.start.type)
            }

            WACCParser.CHR -> {
                if (assocExpr.getType() != WACCType.INT) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "CHR operation only takes INT " +
                                    "(expected: INT, actual: ${assocExpr.getType()})", ctx)
                }
                WACCUnOperExpr(WACCType.CHAR, assocExpr, ctx.start.type)
            }

            WACCParser.BITWISE_NOT -> {
                if (assocExpr.getType() != WACCType.INT) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "BITWISE NOT operation only takes INT " +
                                    "(expected: INT, actual: ${assocExpr.getType()})", ctx)
                }
                WACCUnOperExpr(WACCType.INT, assocExpr, ctx.start.type)
            }
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX, "Invalid unary operator", ctx)
        }
    }

    /**
     * Check that a binary operator expression is valid.
     *
     * This replaces visitBinaryOper() from the base class because
     * binaryOper isn't actually used in the ANTLR grammar.
     * This is manually called in visitExpr().
     */
    private fun checkBinaryOperExpr(ctx: WACCParser.ExprContext): WACCIdentifier {
        if (ctx.bop == null) {
            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                    "Invalid binary operator expression: no operator", ctx)
        }
        if (ctx.expr().size != 2) {
            return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                    "Invalid binary operator expression: " +
                            "must have two operands, found ${ctx.expr().size}", ctx)
        }

        // Analyses the lhs and rhs expressions
        val lhs = ctx.lhs
        val rhs = ctx.rhs

        val lhsIdentifier = visit(lhs)
        val rhsIdentifier = visit(rhs)

        val lhsType: WACCType = lhsIdentifier.getType()
        val rhsType: WACCType = rhsIdentifier.getType()

        // Generates a WACCBinOperExpr based on the binary operator symbol found
        return when (ctx.bop.type) {
            // Mathematical Operators

            WACCParser.MULTIPLY,
            WACCParser.DIVIDE,
            WACCParser.MODULO,
            WACCParser.PLUS,
            WACCParser.MINUS,
            WACCParser.BITWISE_AND,
            WACCParser.BITWISE_OR -> {
                if (lhsType != WACCType.INT) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${lhs.text}\" " +
                                    "(expected: INT, actual: $lhsType)", ctx)
                } else if (rhsType != WACCType.INT) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${rhs.text}\" " +
                                    "(expected: INT, actual: $rhsType)", ctx)
                }
                WACCBinOperExpr(WACCType.INT, lhsIdentifier, rhsIdentifier, ctx.bop.type)
            }

            // Comparator Operators

            WACCParser.GREATER,
            WACCParser.GREATER_EQUAL,
            WACCParser.LESS,
            WACCParser.LESS_EQUAL -> {
                if (lhsType != WACCType.INT && lhsType != WACCType.CHAR) {
                    return if (rhsType != WACCType.INT && rhsType != WACCType.CHAR) {
                        // lhsType and rhsType not comparable
                        errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Incompatible type at \"${lhs.text}\"", ctx)
                    } else {
                        // lhsType not comparable, rhsType comparable
                        errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Incompatible type at \"${lhs.text}\" " +
                                        "(expected: $rhsType, actual: $lhsType)", ctx)
                    }
                } else if (rhsType != lhsType) {
                    // lhsType comparable, rhsType not comparable or rhsType != lhsType
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${rhs.text}\" " +
                                    "(expected: $lhsType, actual: $rhsType)", ctx)
                }
                WACCBinOperExpr(WACCType.BOOL, lhsIdentifier, rhsIdentifier, ctx.bop.type)
            }
            WACCParser.EQUAL,
            WACCParser.NOT_EQUAL -> {
                if (lhsType != rhsType) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible types of \"${lhs.text}\" and \"${rhs.text}\"", ctx)
                }
                WACCBinOperExpr(WACCType.BOOL, lhsIdentifier, rhsIdentifier, ctx.bop.type)
            }
            WACCParser.AND,
            WACCParser.OR -> {
                if (lhsType != WACCType.BOOL) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${lhs.text}\" " +
                                    "(expected: BOOL, actual: $lhsType)", ctx)
                } else if (rhsType != WACCType.BOOL) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${rhs.text}\" " +
                                    "(expected: BOOL, actual: $rhsType)", ctx)
                }
                WACCBinOperExpr(WACCType.BOOL, lhsIdentifier, rhsIdentifier, ctx.bop.type)
            }
            WACCParser.STRING_CONCAT,
            WACCParser.STRING_CONTAINS -> {
                if (lhsType != WACCType.ARRAY) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${lhs.text}\" " +
                                    "(expected: STRING, actual: $lhsType)", ctx)
                }
                if (rhsType != WACCType.ARRAY) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "Incompatible type at \"${rhs.text}\" " +
                                    "(expected: STRING, actual: $rhsType)", ctx)
                }
                if (lhsIdentifier !is WACCStringLiter) {
                    val entry = symbolTable.lookupAll(lhs.text)
                    if (entry !is WACCStringLiter) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Incompatible type at \"${lhs.text}\" " +
                                        "(expected: STRING, actual: $lhsType)", ctx)
                    }
                }
                if (rhsIdentifier !is WACCStringLiter) {
                    val entry = symbolTable.lookupAll(rhs.text)
                    if (entry !is WACCStringLiter) {
                        return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                                "Incompatible type at \"${rhs.text}\" " +
                                        "(expected: STRING, actual: $rhsType)", ctx)
                    }
                }

                WACCBinOperExpr(WACCType.ARRAY, lhsIdentifier, rhsIdentifier, ctx.bop.type)
            }
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX, "Invalid binary operator", ctx)
        }
    }

    /************
     * Literals *
     ************/

    /**
     * Visit identifier.
     */
    override fun visitIdent(ctx: WACCParser.IdentContext): WACCIdentifier {
        val ident: String = ctx.IDENT().text
        val contextIdent = symbolTable.lookupAll(ident)
                ?: return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                        "$ident was used before its declaration", ctx)

        return WACCIdent(contextIdent.getType(), ident)
    }

    /**
     * Visit integer literal.
     */
    override fun visitIntLiter(ctx: WACCParser.IntLiterContext): WACCIdentifier {
        var value: Long = when {
            ctx.binaryIntLiter() != null -> {
                val binaryIntLiter: String = ctx.binaryIntLiter().BIN_INT_LITER().text.removeRange(0, 2)
                binaryIntLiter.toLongOrNull(2) ?: return errorHandler.visitorError(
                        WACCErrorType.SYNTAX,"Invalid assignRhs format", ctx)
            }
            ctx.octalIntLiter() != null -> {
                val octalIntLiter: String = ctx.octalIntLiter().OCT_INT_LITER().text.removeRange(0, 1)
                octalIntLiter.toLongOrNull(8) ?: return errorHandler.visitorError(
                        WACCErrorType.SYNTAX,"Invalid assignRhs format", ctx)
            }
            ctx.decimalIntLiter() != null -> {
                val decimalIntLiter: String = ctx.decimalIntLiter().DEC_INT_LITER().text
                decimalIntLiter.toLongOrNull() ?: return errorHandler.visitorError(
                        WACCErrorType.SYNTAX,"Invalid assignRhs format", ctx)
            }
            ctx.hexIntLiter() != null -> {
                val hexIntLiter: String = ctx.hexIntLiter().HEX_INT_LITER().text.removeRange(0, 2)
                hexIntLiter.toLongOrNull(16) ?: return errorHandler.visitorError(
                        WACCErrorType.SYNTAX,"Invalid assignRhs format", ctx)
            }
            else -> return errorHandler.visitorError(
                    WACCErrorType.SYNTAX,"Invalid int literal format", ctx)
        }

        if (-value < Int.MIN_VALUE) {
            return errorHandler.visitorError(WACCErrorType.SYNTAX,
                    "$value is too large to store as an Int", ctx)
        }

        if (ctx.children[0].text == "-") {
            value = -value
        }

        return WACCIntLiter(value)
    }

    /**
     * Check boolean literal (true/false).
     */
    override fun visitBoolLiter(ctx: WACCParser.BoolLiterContext): WACCIdentifier {
        return when (ctx.BOOL_LITER().symbol.text) {
            "true" -> WACCBoolLiter(true)
            "false" -> WACCBoolLiter(false)
            else -> errorHandler.visitorError(WACCErrorType.SYNTAX,
                    "Invalid boolean literal. " +
                            "Found ${ctx.BOOL_LITER().symbol.text}. Must be 'true' or 'false'", ctx)
        }
    }

    /**
     * Visit character.
     */
    override fun visitCharLiter(ctx: WACCParser.CharLiterContext): WACCIdentifier {
        val encoded = unescapeString(removeSingleQuotes(ctx.CHAR_LITER().text))
        assert(encoded.length == 1) // should always be true unless there's a bug in the lexer/parser grammar
        return WACCCharLiter(encoded[0])
    }

    /**
     * Check string literal.
     */
    override fun visitStrLiter(ctx: WACCParser.StrLiterContext): WACCIdentifier {
        val encoded = unescapeString(removeDoubleQuotes(ctx.STRING_LITER().text))
        return WACCStringLiter(encoded)
    }

    /**
     * Visit array literal.
     */
    override fun visitArrayLiter(ctx: WACCParser.ArrayLiterContext): WACCIdentifier {
        val elems: MutableList<WACCIdentifier> = mutableListOf()
        val arrayElem = ctx.expr(0)

        // Ensure that all array elements have the same type
        if (arrayElem != null) {
            val sampleIden: WACCIdentifier = visit(ctx.expr(0))

            for (expression in ctx.expr()) {
                val testIden = visit(expression)

                if (testIden.getType() != sampleIden.getType()) {
                    return errorHandler.visitorError(WACCErrorType.SEMANTIC,
                            "array elements must be of the same type", ctx)
                }

                elems.add(testIden)
            }

            return WACCArrayLit(elems, sampleIden.getType())
        } else {
            return WACCArrayLit(elems, WACCType.NONE)
        }
    }

    /**
     * Check pair literal "null".
     */
    override fun visitPairLiter(ctx: WACCParser.PairLiterContext): WACCIdentifier {
        return WACCPairLiter()
    }

    /**
     * Create a new WACCPair based on two ExprContexts (passed together as a list).
     */
    private fun makeNewPair(exprContxts: MutableList<WACCParser.ExprContext>): WACCIdentifier {
        val type1 = visit(exprContxts[0])
        val type2 = visit(exprContxts[1])

        return WACCPair(type1, type2)
    }
}
