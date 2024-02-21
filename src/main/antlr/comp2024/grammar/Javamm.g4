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

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : '0' | ([1-9][0-9]*);
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : classDecl EOF
    ;

classDecl
    : CLASS name=ID
        LCURLY
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INTEGER ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    | type name='int''['']'
    | type name='int''...'
    | type name='boolean'
    | type name='int'
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | LCURLY (stmt)* RCURLY #CurlyStmt
    | 'if''('expr')'stmt'else'stmt #IfElseStmt
    | 'while''('expr')'stmt #WhileStmt
    | expr';' #SemiColonStmt
    | ID'='expr';' #IDAssignStmt
    | ID'['expr']''='expr ';' #IDCurlyAssignStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : expr op= (LESS | AND) expr #BinaryExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr LRECT expr RRECT #BinaryExpr
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    | LPAREN expr RPAREN #ParenthesisExpr
    | LCURLY expr RCURLY #ParentCurlyExpr
    | expr '.' ID LPAREN ( expr ( ',' expr )* )? RPAREN #GetMethod
    | LRECT (expr ( ',' expr)* )? RRECT #List
    | NEW INT LRECT expr RRECT #NewInt //
    | NEW ID LRECT RRECT #NewID //
    | expr '.' LENGTH #GetLength //
    | '!' expr #NotExpr //
    | INTEGER #Integer
    | FALSE #FalseExpr
    | TRUE #TrueExpr
    | THIS #ThisExpr
    ;



