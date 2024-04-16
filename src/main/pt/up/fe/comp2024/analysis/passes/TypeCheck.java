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
        addVisit(Kind.GET_METHOD, this::undefMethod);
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
        currentMethod = node.get("name");
        var returntype = node.getChild(0);
        var vardecls = node.getChildren(Kind.VAR_DECL);
        for (var varDecl : vardecls) {
            var name = varDecl.get("name");
            var checkArray = varDecl.getChild(0).get("isArray");
            var value = varDecl.getChild(0).get("value");
            var assignstmt = node.getChildren(Kind.I_D_ASSIGN_STMT);
            for (var stmt : assignstmt) {
                var stmtname = stmt.get("name");
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
                else if (Objects.equals(stmtname, name) && Objects.equals(stmt.getChild(0).isInstance(Kind.I_D_EXPR), true)){
                    var idexpr = stmt.getChild(0);
                    var idname = idexpr.get("name");
                    var assigns = node.getChildren(Kind.VAR_DECL);
                    for (var assign : assigns){
                        if( Objects.equals(idname, assign.get("name"))){
                            var assignType = assign.getChild(0);
                            var imports = table.getImports();
                            Boolean varIsImported = false;
                            Boolean assignIsImported = false;
                            for( var imp : imports){
                                if(Objects.equals(imp, assignType.get("value"))) {
                                    assignIsImported = true;
                                }
                                if(Objects.equals(imp, varDecl.getChild(0).get("value"))){
                                    varIsImported = true;
                                }
                            }
                            if(varIsImported && assignIsImported){
                                continue;
                            }
                            if((Objects.equals(assignType.get("value"), table.getClassName()) && Objects.equals(varDecl.getChild(0).get("value"), table.getSuper())) || (Objects.equals(assignType.get("value"), table.getSuper()) && Objects.equals(varDecl.getChild(0).get("value"), table.getClassName()))){
                                continue;
                            }
                            if(!Objects.equals(assignType, varDecl.getChild(0))){
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        NodeUtils.getLine(node),
                                        NodeUtils.getColumn(node),
                                        "Incompatible types: " + assignType + " and " + varDecl.getChild(0),
                                        null
                                ));
                            }
                        }
                    }
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

    public Void undefMethod(JmmNode node, SymbolTable table) {
        var methodName = node.get("value");
        var methods = table.getMethods();
        var varName = node.getChild(0).get("name");

        for(var imp : table.getImports()){
            for ( var field : table.getFields()){
                if(Objects.equals(field.getName(), varName) && Objects.equals(field.getType().getName(), imp))
                    return null;
            }
            for( var param : table.getParameters(currentMethod)){
                if(Objects.equals(param.getName(), varName) && Objects.equals(param.getType().getName(), imp))
                    return null;
            }
            for( var local : table.getLocalVariables(currentMethod)){
                if(Objects.equals(local.getName(), varName) && Objects.equals(local.getType().getName(), imp))
                    return null;
            }
        }

        if (!methods.contains(methodName) && Objects.equals(table.getSuper(), null)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Method not defined: " + methodName,
                    null
            ));
        }
        return null;
    }
}
