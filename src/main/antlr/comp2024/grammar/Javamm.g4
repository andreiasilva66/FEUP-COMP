grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
NOT : '!' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LRECT : '[' ;
RRECT : ']' ;
LESS : '<' ;
AND : '&&' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

LENGTH : 'length' ;
THIS : 'this' ;
NEW : 'new' ;

TRUE : 'true' ;
FALSE : 'false' ;

IMPORT : 'import' ;
EXTENDS : 'extends' ;
CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
STATIC : 'static' ;
VOID : 'void' ;
MAIN : 'main' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
DOT : '.' ;
COMMA : ',' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : '0' | ([1-9][0-9]*);
ID : [a-zA-Z_$]([a-zA-Z_0-9$])* ;
STRING_ARRAY : 'String' '[' ']' ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : stmt + EOF
    | (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT ID ( DOT ID )* SEMI
    ;

classDecl
    : CLASS name=ID
        ( EXTENDS ID )?
        LCURLY
        ( varDecl )* ( methodDecl )*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN ( type name=ID (COMMA type name=ID)* )? RPAREN
        LCURLY ( varDecl )* ( stmt )* RETURN expr SEMI RCURLY
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN LPAREN STRING_ARRAY ID RPAREN
        LCURLY ( varDecl )* ( stmt )* RCURLY
    ;

type
    : name= INT LRECT RRECT
    | name= INT'...'
    | name= BOOLEAN
    | name= INT
    | name=ID
    ;

stmt
    : LCURLY (stmt)* RCURLY #CurlyStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #SemiColonStmt
    | ID EQUALS expr SEMI #IDAssignStmt
    | ID LRECT expr RRECT EQUALS expr SEMI #IDCurlyAssignStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : NOT expr #NotExpr
    | expr op= (LESS | AND) expr #BinaryExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr LRECT expr RRECT #BinaryExpr
    | expr DOT LENGTH #GetLength
    | expr DOT ID LPAREN ( expr ( COMMA expr )* )? RPAREN #GetMethod
    | NEW INT LRECT expr RRECT #NewInt //
    | NEW ID LPAREN RPAREN #NewID
    | LPAREN expr RPAREN #ParenthesisExpr
    | LRECT (expr ( COMMA expr)* )? RRECT #List
    | INTEGER #Integer
    | TRUE #TrueExpr
    | FALSE #FalseExpr
    | ID #IDExpr
    | THIS #ThisExpr
    ;



