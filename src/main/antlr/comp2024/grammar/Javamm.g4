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

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        returnType name=ID
        LPAREN ( type argName=ID (COMMA type argName=ID)* )? RPAREN
        LCURLY ( varDecl )* ( stmt )* RETURN expr SEMI RCURLY
    | (PUBLIC {$isPublic=true;})?
        'static' returnType name=MAIN LPAREN type argName=ID RPAREN
        LCURLY ( varDecl )* ( stmt )* RCURLY
    ;

type locals[boolean isArray=false]
    : (value=INT'['']' {$isArray=true;})
    | (value=STRING'['']' {$isArray=true;})
    | value=INT'...'
    | value=BOOLEAN
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
    | var=ID EQUALS expr SEMI #IDAssignStmt
    | var=ID LRECT expr RRECT EQUALS expr SEMI #IDCurlyAssignStmt
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
    | value=(TRUE | FALSE) #BOOLEAN
    | name=ID #IDExpr
    | THIS #ThisExpr
    | expr DOT LENGTH #GetLength
    | expr DOT value=ID LPAREN ( expr ( COMMA expr )* )? RPAREN #GetMethod
    | LRECT (expr ( COMMA expr)* )? RRECT #List
    ;



