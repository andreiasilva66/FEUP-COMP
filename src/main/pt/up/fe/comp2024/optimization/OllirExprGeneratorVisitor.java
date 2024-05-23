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
        addVisit(INTEGER_EXPR, this::visitInteger);
        addVisit(NEW_I_D, this::visitNewID);
        addVisit(BOOLEAN_EXPR, this::visitBoolean);
        addVisit(BINARY_BOOL_EXPR, this::visitBinBoolExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitBinBoolExpr(JmmNode node, Void unused){
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

        var temp = OptUtils.getTemp();

        computation.append(temp);
        computation.append(resOllirType);
        computation.append(SPACE);
        computation.append(ASSIGN);
        computation.append(resOllirType);
        computation.append(SPACE);
        computation.append(lhs.getCode());
        computation.append(SPACE);
        computation.append(node.get("op"));
        computation.append(resOllirType);
        computation.append(SPACE);
        computation.append(rhs.getCode());
        computation.append(END_STMT);

        code.append(temp);
        code.append(resOllirType);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        if(node.get("value").equals("true")){
            return new OllirExprResult("1" + ollirBoolType);
        }
        return new OllirExprResult("0" + ollirBoolType);
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
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        var methodName = node.get("value");
        var temp = OptUtils.getTemp();

        if(table.getMethods().stream().anyMatch(method -> method.equals(methodName))){
            var returnType = table.getReturnType(methodName);
            computation.append(temp);
            computation.append(OptUtils.toOllirType(returnType));
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(OptUtils.toOllirType(returnType));
            computation.append(SPACE);
            computation.append("invokevirtual(");
            computation.append(node.getChild(0).get("name"));
            computation.append(".").append(table.getClassName());
            computation.append(", \"").append(methodName).append("\"");
            for(int i = 1; i < node.getNumChildren(); i++){
                var expr = visit(node.getChild(i));
                computation.append(", ");
                computation.append(expr.getComputation());
                computation.append(expr.getCode());

            }
            computation.append(")").append(OptUtils.toOllirType(returnType));
            computation.append(END_STMT);
            code.append(temp);
            code.append(OptUtils.toOllirType(returnType));
        }
        else{
            String returnType = "";
            if (node.getChildren(I_D_EXPR).size() == 2) {
                var varName = node.getChildren(I_D_EXPR).get(1);
                computation.append(", ");
                computation.append(varName.get("name"));
                // get the return type of the variable
                var locals = table.getLocalVariables(node.getJmmParent().getJmmParent().get("name"));
                for (var local : locals) {
                    if (local.getName().equals(varName.get("name"))) {
                        returnType = OptUtils.toOllirType(local.getType());
                        computation.append(OptUtils.toOllirType(local.getType()));
                    }
                }
            }
            computation.append(temp);
            computation.append(returnType);
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(returnType);
            computation.append(SPACE);
            computation.append("invokestatic(");
            var classMethod = node.getChildren(I_D_EXPR).get(0);
            computation.append(classMethod.get("name"));
            computation.append(", ");
            computation.append("\"");
            computation.append(methodName);
            computation.append("\"");

            computation.append(").");
            computation.append("V");
            computation.append(END_STMT);
            code.append(temp);
            code.append(returnType);
        }

        return new OllirExprResult(code.toString(), computation.toString());
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

        var temp = OptUtils.getTemp();

        computation.append(temp);
        computation.append(resOllirType);
        computation.append(SPACE);
        computation.append(ASSIGN);
        computation.append(resOllirType);
        computation.append(SPACE);
        computation.append(lhs.getCode());
        computation.append(SPACE);
        computation.append(node.get("op"));
        computation.append(resOllirType);
        computation.append(SPACE);
        computation.append(rhs.getCode());
        computation.append(END_STMT);

        code.append(temp);
        code.append(resOllirType);

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
