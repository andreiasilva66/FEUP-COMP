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
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.I_D_EXPR, this::visitVarRefExpr);
    }

    private Void multTypes(JmmNode node, SymbolTable table) {

        var left = node.getChild(0);
        var right = node.getChild(1);
        var leftType = left.getChildren(Kind.TYPE);
        var rightType = right.getChildren(Kind.TYPE);
        if (!leftType.equals(rightType)) {
            addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getCol(node),
                "Incompatible types: " + leftType + " and " + rightType
            ));
        }
        return null;
    }
}
