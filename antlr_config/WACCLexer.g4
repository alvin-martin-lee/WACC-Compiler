lexer grammar WACCLexer;

// skip
COMMENT: HASH (NON_EOL_CHAR)* EOL -> skip;
WHITESPACE : (' ' | '\t' | '\r' | '\n')+ -> channel(HIDDEN);

// control
BEGIN: 'begin';
END: 'end';
IS: 'is';
SKIP_: 'skip';   // SKIP is ANTLER reserved word
READ: 'read';
FREE: 'free';
RETURN: 'return';
EXIT: 'exit';
PRINT: 'print';
PRINTLN: 'println';
IF: 'if';
THEN: 'then';
ELSE: 'else';
FI: 'fi';
WHILE: 'while';
DO: 'do';
DONE: 'done';
CALL: 'call';
BREAK: 'break';
FOR: 'for';
PROCEED: 'proceed';
SWITCH: 'switch';
WHERE: 'where';
CASE: 'case';
ESAC: 'esac';

// standard library func
POW: 'pow';
MAX: 'max';
MIN: 'min';
SQUARE: 'square';
CUBE: 'cube';
HYPO: 'hype';

// "standard library": string-handling
STRLEN: 'strlen';
STRLOWER: 'lowercase';
STRUPPER: 'uppercase';

// punctuation
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
OPEN_SQUARE: '[';
CLOSE_SQUARE: ']';
COMMA: ',';
SEMICOLON : ';' ;
HASH: '#';

// assign
ASSIGN: '=';

// pair
NEWPAIR: 'newpair';
FST: 'fst';
SND: 'snd';
PAIR: 'pair';

// types
INT: 'int';
BOOL: 'bool';
CHAR: 'char';
STRING: 'string';

// unary
NOT: '!';
LEN: 'len';
ORD: 'ord';
CHR: 'chr';

// binary
MULTIPLY: '*';
DIVIDE: '/';
MODULO: '%';
PLUS: '+';
MINUS: '-';
GREATER: '>';
GREATER_EQUAL: '>=';
LESS: '<';
LESS_EQUAL: '<=';
EQUAL: '==';
NOT_EQUAL: '!=';
AND: '&&';
OR: '||';

// bitwise binary
BITWISE_AND: '&';
BITWISE_OR: '|';
BITWISE_NOT: '~';

// String-handling expressions (ext.)
STRING_CONCAT: '++';
STRING_CONTAINS: 'in';

// boolean
fragment TRUE: 'true';
fragment FALSE: 'false';
BOOL_LITER: TRUE | FALSE;

// null
NULL: 'null';

// character
fragment UNDERSCORE: '_';
fragment LOWER: 'a'..'z';
fragment UPPER: 'A'..'Z';
fragment NUMERAL: '0'..'9';
fragment ESCAPE_CHAR: ('0' | 'b' | 't' | 'n' | 'f' | 'r' | '\\' | '\'' | '"');
fragment CHARACTER: ~('\\' | '\'' | '"') | ('\\' ESCAPE_CHAR);

// int liter prefixes
BIN_INT_PREFIX: '0b' | '0B';
HEX_INT_PREFIX: '0x' | '0X';

// Note: for INT_LITER, the optional sign is checked in the parser
BIN_INT_LITER: BIN_INT_PREFIX ('0' | '1')+;
OCT_INT_LITER: '0' ('0'..'7')+;
DEC_INT_LITER: '0' | ('1'..'9' NUMERAL*);
HEX_INT_LITER: HEX_INT_PREFIX (NUMERAL | 'a'..'f' | 'A'..'F')+;
CHAR_LITER: '\'' CHARACTER '\'';
STRING_LITER: '"' (CHARACTER)* '"';

// ident
fragment IDENT_CHAR_FST: UNDERSCORE | LOWER | UPPER;
fragment IDENT_CHAR: IDENT_CHAR_FST | NUMERAL;
IDENT: IDENT_CHAR_FST (IDENT_CHAR)*;

// EOL (End Of Line)
NON_EOL_CHAR: ~('\r' | '\n');
EOL: ('\r' | '\n');
