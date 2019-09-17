package wacc

/* Kotlin doesn't allow writing \0 as a literal. */
val NULL_CHAR = 0.toChar()

/* For converting escaped characters. */
val escapedCharacters: List<Pair<String, String>> = listOf(
        Pair("\\0", NULL_CHAR.toString()),
        Pair("\\t", "\t"),
        Pair("\\b", "\b"),
        Pair("\\n", "\n"),
        Pair("\\r", "\r"),
        Pair("\\\'", "\'"),
        Pair("\\\"", "\""),
        Pair("\\\$", "\$")
)

/**
 * Convert characters in a string that should have been escaped.
 *
 * In the ANTLR lexer/parser, strings with escaped characters are treated as if they weren't.
 * For example when ANTLR reads the string "hello world\n", it treats "\n" as two separate
 * characters '\' and 'n'. We want it to represent the newline '\n' instead.
 */
fun unescapeString(string: String): String {
    var res = string
    for ((from, to) in escapedCharacters) {
        res = res.replace(from, to)
    }
    return res
}

/**
 * Convert a string with to display all escaped characters with backslashes.
 *
 * For example if a string contains the character '\n', it will be converted to "\n"
 * to show both the backslash '\' and the escaped character 'n'.
 *
 * If the string is added as is, then when writing it to the output assembly file
 * they would disappear (e.g. \n is replaced with an actual newline, \0 appears as ^@ on Linux)
 * We want to display such characters in full.
 */
fun escapeString(string: String): String {
    var res = string
    for ((to, from) in escapedCharacters) {
        res = res.replace(from, to)
    }
    return res
}

/**
 * Remove surrounding single quotes around a string.
 */
fun removeSingleQuotes(string: String): String {
    return string.removeSurrounding("\'")
}

/**
 * Remove surrounding double-quotes from a string.
 */
fun removeDoubleQuotes(string: String): String {
    return string.removeSurrounding("\"")
}