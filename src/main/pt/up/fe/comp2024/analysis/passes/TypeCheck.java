package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;

public class TypeCheck extends AnalysisVisitor {

    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::binTypes);
        addVisit(Kind.METHOD_DECL, this::listTypes);
        addVisit(Kind.CLASS_DECL, this::checkDeclaredMethods);
    }

    private Void binTypes(JmmNode node, SymbolTable table) {

        var left = node.getChild(0);
        var right = node.getChild(1);
        var leftType = left.getChildren(Kind.TYPE);
        var rightType = right.getChildren(Kind.TYPE);
        if (leftType.equals("int") || leftType.equals("float")) {
            if (rightType.equals("int") || rightType.equals("float")) {
                return null;
            } else {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + leftType + " and " + rightType,
                        null
                ));
            }
        } else {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Incompatible types: " + leftType + " and " + rightType,
                    null
            ));
        }
        return null;
    }

    private Void listTypes(JmmNode node, SymbolTable table) {
        var returntype = node.getChild(0);
        var vardecls = node.getChildren(Kind.VAR_DECL);
        for (var varDecl : vardecls) {
            var name = varDecl.get("name");
            var checkArray = varDecl.getChild(0).get("isArray");
            var value = varDecl.getChild(0).get("value");
            var assignstmt = node.getChildren(Kind.I_D_ASSIGN_STMT);
            for (var stmt : assignstmt) {
                var stmtname = stmt.get("name");
                System.out.println("StmtName: " + stmtname);
                if (Objects.equals(stmtname, name) && Objects.equals(value, "boolean") && Objects.equals(stmt.getChild(0).isInstance(Kind.INTEGER_EXPR), true)) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Can't assign int to bool: " + value + " and " + stmt.getChild(0),
                            null
                    ));
                }
                if (Objects.equals(stmtname, name) && (Objects.equals(value, "int") || Objects.equals(value, "float")) && Objects.equals(stmt.getChild(0).isInstance(Kind.BOOLEAN_EXPR), true)) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Can't assign bool to int: " + value + " and " + stmt.getChild(0),
                            null
                    ));
                }
            }
            var whilestmts = node.getChildren(Kind.WHILE_STMT);
            for (var whileStmt : whilestmts) {
                var idexprs = whileStmt.getChildren(Kind.I_D_EXPR);
                for (var idexpr : idexprs) {
                    var idname = idexpr.get("name");
                    if (Objects.equals(name, idname) && checkArray.equals("true")) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(node),
                                NodeUtils.getColumn(node),
                                "Can't do a while statement with array: " + node.getChildren(Kind.TYPE),
                                null
                        ));
                    }
                }
            }
        }
        var assignstmt = node.getChildren(Kind.I_D_ASSIGN_STMT);
        System.out.println("IDAssignStmt: " + assignstmt);
        for (var stmt : assignstmt) {
            var children = stmt.getChildren();
            var id = children.get(0);
            if (returntype.getChild(0).get("isArray").equals("false") && Objects.equals(id.toString(), "List")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Can't initiate an array when it does not exist: " + node.getChildren(Kind.TYPE),
                        null
                ));
            }
            if (Objects.equals(id.toString(), "List")) {
                var child = id.getChildren();
                var firstExpr = child.get(0);
                for (var c : child) {
                    if (c.toString().equals(firstExpr.toString())) {
                        continue;
                    } else {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(node),
                                NodeUtils.getColumn(node),
                                "Different types: " + firstExpr + " and " + c,
                                null
                        ));
                    }
                }
            }
        }

        return null;
    }

    private Void checkDeclaredMethods(JmmNode node, SymbolTable table) {
        int counter = 0;
        Boolean checkMethod = false;
        var classname = node.get("className");
        var methods = node.getChildren(Kind.METHOD_DECL);
        for (var method : methods) {
            var vardecls = method.getChildren(Kind.VAR_DECL);
            for (var varDecl : vardecls) {
                var typevalue = varDecl.getChild(0).get("value");
                if (Objects.equals(typevalue, classname)) {
                    checkMethod = true;
                    var semicolonstmts = method.getChildren(Kind.SEMI_COLON_STMT);
                    for (var semicolonstmt : semicolonstmts) {
                        var getmethodvalue = semicolonstmt.getChild(0).get("value");
                        if (Objects.equals(method.get("name"), getmethodvalue)) {
                            counter++;
                        }
                    }
                }
            }
        }
        if (checkMethod && counter == 0) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Method not declared: " + node.getChildren(Kind.METHOD_DECL),
                    null
            ));
        }
        return null;
    }
}
