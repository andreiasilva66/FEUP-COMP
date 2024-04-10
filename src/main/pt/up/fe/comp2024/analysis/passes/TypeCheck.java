package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.I_D_ASSIGN_STMT;

public class TypeCheck extends AnalysisVisitor {

    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::binTypes);
        addVisit(Kind.METHOD_DECL, this::listTypes);
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
                    }
                    else {
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
}
