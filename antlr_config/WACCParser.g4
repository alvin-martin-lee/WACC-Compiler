parser grammar WACCParser;

options {
  tokenVocab=WACCLexer;
}

program
    // EOF indicates that the program must consume to the end of the input.
    : BEGIN (func)* stat END EOF
    ;

func
    : type ident OPEN_PAREN (paramList)? CLOSE_PAREN IS stat END
    ;

paramList
    : param (COMMA param)*
    ;

param
    : type ident
    ;

stat
    : SKIP_
    | BREAK
    | PROCEED
    | type ident ASSIGN assignRhs
    | assignLhs ASSIGN assignRhs
    | READ assignLhs
    | FREE expr
    | RETURN expr
    | EXIT expr
    | PRINT expr
    | PRINTLN expr
    | STRLEN expr
    | STRLOWER expr
    | STRUPPER expr
    | IF expr THEN thenBody=stat ELSE elseBody=stat FI
    | IF expr THEN thenBody=stat FI
    | WHILE expr DO stat DONE
    | DO stat WHILE expr DONE
    | FOR OPEN_PAREN stat SEMICOLON expr SEMICOLON stat CLOSE_PAREN DO stat DONE
    | BEGIN stat END
    | SWITCH ident WHERE (switchCase)+ DONE
    | currentStat=stat SEMICOLON nextStat=stat
    ;

switchCase
    : CASE expr DO stat ESAC
    ;

assignLhs
    : ident
    | arrayElem
    | pairElem
    ;

assignRhs
    : rhsExpr=expr
    | arrayLiter
    | NEWPAIR OPEN_PAREN expr COMMA expr CLOSE_PAREN
    | pairElem
    | CALL ident OPEN_PAREN argList? CLOSE_PAREN
    ;

argList
    : expr (COMMA expr)*
    ;

pairElem
    : FST expr
    | SND expr
    ;

type
    : baseType
    | type OPEN_SQUARE CLOSE_SQUARE
    | pairType
    ;

baseType
    : INT
    | BOOL
    | CHAR
    | STRING
    ;

pairType
    : PAIR OPEN_PAREN fst=pairElemType COMMA snd=pairElemType CLOSE_PAREN
    ;

pairElemType
    : baseType
    | type OPEN_SQUARE CLOSE_SQUARE
    | PAIR
    ;

expr
    : intLiter
    | boolLiter
    | charLiter
    | strLiter
    | pairLiter
    | ident
    | arrayElem
    | standardLibFunc
    | uop=unaryOper expr
    | lhs=expr bop=(MULTIPLY | DIVIDE | MODULO) rhs=expr
    | lhs=expr bop=(PLUS | MINUS) rhs=expr
    | lhs=expr bop=(GREATER | GREATER_EQUAL | LESS | LESS_EQUAL) rhs=expr
    | lhs=expr bop=(EQUAL | NOT_EQUAL) rhs=expr
    | lhs=expr bop=AND rhs=expr
    | lhs=expr bop=OR rhs=expr
    | lhs=expr bop=STRING_CONCAT rhs=expr
    | lhs=expr bop=STRING_CONTAINS rhs=expr
    | lhs=expr bop=(BITWISE_AND | BITWISE_OR) rhs=expr
    | OPEN_PAREN parenExpr=expr CLOSE_PAREN
    ;

standardLibFunc
    : standardMathFunc
    ;

standardMathFunc
    : POW OPEN_PAREN expr COMMA expr CLOSE_PAREN
    | MAX OPEN_PAREN expr COMMA expr CLOSE_PAREN
    | MIN OPEN_PAREN expr COMMA expr CLOSE_PAREN
    | SQUARE OPEN_PAREN expr CLOSE_PAREN
    | CUBE OPEN_PAREN expr CLOSE_PAREN
    | HYPO OPEN_PAREN expr COMMA expr CLOSE_PAREN
    ;

unaryOper
    : NOT
    | MINUS
    | LEN
    | ORD
    | CHR
    | BITWISE_NOT
    ;

ident
    : IDENT
    ;

arrayElem
    : ident (OPEN_SQUARE expr CLOSE_SQUARE)+
    ;

intLiter
    : binaryIntLiter
    | octalIntLiter
    | decimalIntLiter
    | hexIntLiter
    ;

binaryIntLiter
    : (PLUS | MINUS)? BIN_INT_LITER
    ;

octalIntLiter
    : (PLUS | MINUS)? OCT_INT_LITER
    ;

decimalIntLiter
    : (PLUS | MINUS)? DEC_INT_LITER
    ;

hexIntLiter
    : (PLUS | MINUS)? HEX_INT_LITER
    ;

boolLiter
    : BOOL_LITER
    ;

charLiter
    : CHAR_LITER
    ;

strLiter
    : STRING_LITER
    ;

arrayLiter
    : OPEN_SQUARE (expr (COMMA expr)*)? CLOSE_SQUARE
    ;

pairLiter
    : NULL
    ;
