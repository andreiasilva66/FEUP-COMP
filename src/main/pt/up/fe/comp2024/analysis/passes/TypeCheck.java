package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class TypeCheck extends AnalysisVisitor {

    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::binTypes);
        addVisit(Kind.ASSIGN_STMT, this::listTypes);
    }

    private Void binTypes(JmmNode node, SymbolTable table) {

        System.out.println("No multTypes");

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
        var type = node.getChild(0);

        System.out.println("Na list Type");

        if(type.toString() == "List"){
            var child = node.getChildren();
            for(var c : child){
                if(c.getChildren(Kind.TYPE).equals(type.getChildren(Kind.TYPE))){
                    continue;
                } else {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Incompatible types: " + node.getChildren(Kind.TYPE),
                            null
                    ));
                }
            }
        }
        return null;
    }
}
