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
    : IMPORT value+=ID ( DOT value+=ID )* SEMI
    ;

classDecl
    : CLASS name=ID
        ( EXTENDS superclassname=ID )?
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
        STATIC VOID name=MAIN LPAREN STRING_ARRAY ID RPAREN
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
    : LPAREN expr RPAREN #ParenthesisExpr
    | expr LRECT expr RRECT #BinaryExpr
    | NOT expr #NotExpr
    | NEW INT LRECT expr RRECT #NewInt //
    | NEW ID LPAREN RPAREN #NewID
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op= LESS expr #BinaryExpr //
    | expr op= AND expr #BinaryExpr
    | INTEGER #Integer
    | (TRUE | FALSE) #BOOLEAN
    | ID #IDExpr
    | THIS #ThisExpr
    | expr DOT LENGTH #GetLength
    | expr DOT ID LPAREN ( expr ( COMMA expr )* )? RPAREN #GetMethod
    | LRECT (expr ( COMMA expr)* )? RRECT #List
    ;



