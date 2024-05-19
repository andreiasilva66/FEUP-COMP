package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(I_D_EXPR, this::visitIDExpr);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(GET_METHOD, this::visitGetMethod);
        addVisit(I_D_ASSIGN_STMT, this::visitAssignStmt);
        //addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(NEW_I_D, this::visitNewID);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitNewID(JmmNode node, Void unused) {
        var type = node.get("value");

        var temp_variable = OptUtils.getTemp();

        String code = temp_variable + "." + type;
        String computation = code + SPACE + ASSIGN + "." + type + SPACE + "new(" + type + ")" + "." + type + END_STMT;
        computation += "invokespecial(" + code + ", \"<init>\").V;\n";
        code += END_STMT;
        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitGetMethod(JmmNode node, Void unused) {
        var id = node.get("value");
        Type type = table.getReturnType(id);
        String ollirType = OptUtils.toOllirType(type);

        var temp_variable = OptUtils.getTemp();

        String code = temp_variable + ollirType;
        String computation = code + SPACE + ASSIGN + ollirType + SPACE + "invokevirtual(";
        if(node.getChild(0).getKind().equals("ThisExpr")){
            computation += "this";
        } else {
            computation += visit(node.getChild(0)).getCode();
        }
        computation += ", \"" + id + "\"";
        for(int i = 1; i < node.getNumChildren(); i++){
            computation += ", " + visit(node.getChild(i)).getCode();
        }
        computation += ")" + ollirType + END_STMT;
        code += END_STMT;
        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitAssignStmt(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        StringBuilder code = new StringBuilder();

        code.append(lhs.getCode());
        code.append(resOllirType).append(SPACE).append(ASSIGN).append(SPACE).append(rhs.getCode()).append(resOllirType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        StringBuilder code = new StringBuilder();

        code.append(lhs.getCode());
        code.append(SPACE);
        code.append(node.get("op"));
        code.append(resOllirType);
        code.append(SPACE);
        code.append(rhs.getCode());
        //code.append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }


    private OllirExprResult visitIDExpr(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType;
        try {
            ollirType = OptUtils.toOllirType(type);
        } catch (Exception e) {
            ollirType = "." + type.getName();
        }

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
