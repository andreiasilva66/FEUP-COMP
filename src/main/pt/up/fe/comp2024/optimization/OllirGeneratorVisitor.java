package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(I_D_ASSIGN_STMT, this::visitAssignStmt);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(GET_METHOD, this::visitGetMethod);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitGetMethod(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var methodName = node.get("value");
        code.append("invokestatic(");
        var classMethod = node.getChildren(I_D_EXPR).get(0);
        var varName = node.getChildren(I_D_EXPR).get(1);
        code.append(classMethod.get("name"));
        code.append(", ");
        code.append("\"");
        code.append(methodName);
        code.append("\"");
        code.append(", ");
        code.append(varName.get("name"));
        // get the return type of the variable
        var locals = table.getLocalVariables(node.getJmmParent().getJmmParent().get("name"));
        for (var local : locals) {
            if (local.getName().equals(varName.get("name"))) {
                code.append(OptUtils.toOllirType(local.getType()));
            }
        }
        code.append(").");
        code.append("V");
        code.append(END_STMT);
        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var child = node.getJmmChild(0);
        var parent = node.getParent();
        if (parent.getKind().equals("ClassDecl")) {
            code.append(".field public ");
        }
        // id should have int, bool or String
        var fieldValue = child.get("value");
        var field = OptUtils.toFieldType(fieldValue);
        code.append(field).append("Field");
        code.append(OptUtils.toOllirType(child));
        code.append(END_STMT);

        return code.toString();
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        return "import " + node.get("ID") + END_STMT;
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var name = node.get("name");
        var methodName = node.getParent().get("name");
        var locals = table.getLocalVariables(methodName);
        for (var local : locals) {
            if (local.getName().equals(name)) {
                if (node.getChild(0).getKind().equals("IntegerExpr")) {
                    code.append(local.getName());
                    code.append(OptUtils.toOllirType(local.getType()));
                    code.append(ASSIGN);
                    code.append(OptUtils.toOllirType(local.getType()));
                    code.append(SPACE);
                    code.append(node.getChild(0).get("value"));
                    code.append(OptUtils.toOllirType(local.getType()));
                    code.append(END_STMT);
                }
                if (node.getChild(0).getKind().equals("IDExpr")) {
                    code.append(local.getName());
                    code.append(OptUtils.toOllirType(local.getType()));
                    code.append(ASSIGN);
                    code.append(OptUtils.toOllirType(local.getType()));
                    code.append(SPACE);
                    code.append(node.getChild(0).get("name"));
                    code.append(OptUtils.toOllirType(local.getType()));
                    code.append(END_STMT);

                }
            }
        }

        // Check if the assignment is to an array element
        if (node.getKind().equals("IDCurlyAssignStmt")) {
            code.append("[");
            var index = exprVisitor.visit(node.getJmmChild(2));
            code.append(index.getCode());
            code.append("]");
        }

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");
        var checkArray = "";
        if (node.getChild(0).get("isArray").equals("true")) {
            checkArray = ".array";
        }

        String code = id + checkArray + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(NL + ".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isPublic) {
            code.append("public ");
        }

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        var childrenParams = node.getChildren(PARAM);
        code.append("(");
        for (var childParam : childrenParams) {
            if (childrenParams.indexOf(childParam) != childrenParams.size() - 1) {
                code.append(visit(childParam));
                code.append(", ");
            } else {
                code.append(visit(childParam));
            }
        }
        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0).getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);
        var idAssignStmts = node.getChildren(I_D_ASSIGN_STMT);
        if (!idAssignStmts.isEmpty()) {
            for (var idAssignStmt : idAssignStmts) {
                code.append(visit(idAssignStmt));
            }
        }
        var semiColonStmt = node.getChildren(SEMI_COLON_STMT);
        if (!semiColonStmt.isEmpty()) {
            for (var stmt : semiColonStmt) {
                code.append(visit(stmt.getJmmChild(0)));
            }
        }
        var returnStmt = node.getChildren(RETURN_STMT);
        if (!returnStmt.isEmpty()) {
            if (returnStmt.get(0).getChild(0).getKind().equals("BinaryExpr")) {
                code.append(visit(returnStmt.get(0)));
            }
            else {
                code.append("ret");
                code.append(retType);
                if (!returnStmt.isEmpty()) {
                    code.append(SPACE);
                    var value = returnStmt.get(0).getJmmChild(0);
                    if (value.getKind().equals("IntegerExpr")) {
                        code.append(value.get("value"));
                    } else {
                        if (value.getKind().equals("BinaryExpr")) {
                            code.append(value.get("op"));
                        } else {
                            code.append(value.get("name"));
                        }
                    }
                    code.append(retType);
                }
                code.append(END_STMT);
            }
        }
        else {
            code.append("ret");
            code.append(retType);
            code.append(END_STMT);
        }

        code.append(R_BRACKET);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        if (table.getSuper() != null) {
            code.append(" extends ");
            code.append(table.getSuper());
        }
        code.append(L_BRACKET);

        code.append(NL);

        for (var child : node.getChildren()) {
            var result = visit(child);

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return NL + ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
