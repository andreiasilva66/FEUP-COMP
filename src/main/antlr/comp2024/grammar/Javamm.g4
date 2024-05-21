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

IMPORT : 'import' ;
EXTENDS : 'extends' ;
CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean' ;
STRING : 'String' ;
VOID : 'void' ;
MAIN : 'main' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
DOT : '.' ;
COMMA : ',' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

BOOLEAN : 'true' | 'false' ;
INTEGER : '0' | ([1-9][0-9]*);
ID : [a-zA-Z_$]([a-zA-Z_0-9$])* ;
LINECOMMENT : '//' .*? '\n' -> skip ;
MULTILINECOMMENT : '/*' .*? '*/' -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : stmt + EOF
    | (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT value+=ID ( DOT value+=ID )* SEMI
    ;

classDecl
    : CLASS className=ID
        ( EXTENDS superClassName=ID )?
        LCURLY
        ( varDecl )* ( methodDecl )*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    | type name=MAIN SEMI
    | type name=ID LRECT RRECT SEMI
    ;

returnType
    : type
    ;

returnStmt
    : RETURN expr SEMI
    ;

param
    : type name=ID
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
        returnType name=ID
        LPAREN ( param (COMMA param)* )? RPAREN
        LCURLY ( varDecl )* ( stmt )* returnStmt RCURLY
    | (PUBLIC {$isPublic=true;})?
        'static' {$isStatic=true;} returnType name=MAIN LPAREN param RPAREN
        LCURLY ( varDecl )* ( stmt )* RCURLY
    ;

type locals[boolean isArray=false]
    : (value=INT'['']' {$isArray=true;})
    | (value=STRING'['']' {$isArray=true;})
    | value=INT'...' {$isArray=true;}
    | value=BOOL
    | value=INT
    | value=STRING
    | value=ID
    | value=VOID
    ;

stmt
    : LCURLY (stmt)* RCURLY #CurlyStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #SemiColonStmt
    | name=ID EQUALS value=expr SEMI #IDAssignStmt
    | name=ID LRECT expr RRECT EQUALS expr SEMI #IDCurlyAssignStmt
    ;

expr
    : LPAREN expr RPAREN #ParenthesisExpr
    | expr LRECT expr RRECT #ArrayExpr
    | NOT expr #NotExpr
    | NEW INT LRECT expr RRECT #NewInt //
    | NEW value=ID LPAREN RPAREN #NewID
    | expr DOT LENGTH #GetLength
    | expr DOT value=ID LPAREN ( expr ( COMMA expr )* )? RPAREN #GetMethod
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op= LESS expr #BinaryExpr //
    | expr op= AND expr #BinaryExpr
    | value=INTEGER #IntegerExpr
    | value=BOOLEAN #BooleanExpr
    | name=ID #IDExpr
    | THIS #ThisExpr
    | LRECT (expr ( COMMA expr)* )? RRECT #List
    ;



