package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.I_D_EXPR;

public class TypeCheck extends AnalysisVisitor {

    private String currentMethod;

    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::classDecl);
        addVisit(Kind.BINARY_EXPR, this::binTypes);
        addVisit(Kind.METHOD_DECL, this::listTypes);
        addVisit(Kind.GET_METHOD, this::getMethod);
        addVisit(Kind.ARRAY_EXPR, this::arrayExpr);
        addVisit(Kind.BINARY_EXPR, this::binExpr);
        addVisit(Kind.RETURN_STMT, this::returnStmt);
        addVisit(Kind.IF_ELSE_STMT, this::ifElseStmt);
        addVisit(Kind.VAR_DECL, this::varDecl);
        addVisit(Kind.BINARY_BOOL_EXPR, this::binBoolExpr);
        addVisit(Kind.I_D_ASSIGN_STMT, this::assignStmt);
    }

    private Void classDecl(JmmNode node, SymbolTable table) {
        var imports = table.getImports();
        // check for duplicated imports
        for (int i = 0; i < imports.size(); i++) {
            for (int j = i + 1; j < imports.size(); j++) {
                if (Objects.equals(imports.get(i), imports.get(j))) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Duplicated imports: " + imports.get(i) + " and " + imports.get(j),
                            null
                    ));
                }
            }
        }
        var fields = table.getFields();
        // check for duplicated fields
        for (int i = 0; i < fields.size(); i++) {
            for (int j = i + 1; j < fields.size(); j++) {
                if (Objects.equals(fields.get(i), fields.get(j))) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Duplicated fields: " + fields.get(i) + " and " + fields.get(j),
                            null
                    ));
                }
            }
        }
        var methods = table.getMethods();
        // check for duplicated methods
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                if (Objects.equals(methods.get(i), methods.get(j))) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Duplicated methods: " + methods.get(i) + " and " + methods.get(j),
                            null
                    ));
                }
            }
        }
        return null;
    }

    private Void assignStmt(JmmNode node, SymbolTable table) {
        var name = node.get("name");
        var method = node.getParent();
        var varDecls = method.getParent().getChildren(Kind.VAR_DECL);
        var checkStatic = method.get("isStatic");
        for (var varDecl : varDecls) {
            var varName = varDecl.get("name");
            if (Objects.equals(varName, name) && Objects.equals(checkStatic, "true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Can't access a non static field from a static method: " + name,
                        null
                ));
            }
        }
        return null;
    }

    private Void binBoolExpr(JmmNode node, SymbolTable table){
        var left = node.getChild(0);
        var right = node.getChild(1);
        var leftKind = left.getKind();
        var rightKind = right.getKind();
        if(leftKind.equals("BooleanExpr") || rightKind.equals("BooleanExpr")){
            return null;
        } else {
            checkBinBoolExpr(node, table, left, leftKind);
        }
        checkBinBoolExpr(node, table, right, rightKind);
        return null;
    }

    private void checkBinBoolExpr(JmmNode node, SymbolTable table, JmmNode left, String leftKind) {
        if (leftKind.equals("IDExpr")){
            String leftType = "";
            boolean isLeftArray = false;
            for(var local : table.getLocalVariables(currentMethod)){
                if(Objects.equals(local.getName(), left.get("name"))){
                    leftType = local.getType().getName();
                    isLeftArray = local.getType().isArray();
                    break;
                }
            }
            if(leftType.isEmpty()) {
                for (var param : table.getParameters(currentMethod)) {
                    if (Objects.equals(param.getName(), left.get("name"))) {
                        leftType = param.getType().getName();
                        isLeftArray = param.getType().isArray();
                        break;
                    }
                }
                if(leftType.isEmpty()){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Variable not defined: " + left.get("name"),
                            null
                    ));
                }
            }
            if(!leftType.equals("boolean") || isLeftArray){
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + leftType + " and " + "boolean",
                        null
                ));
            }
        }
        else if(leftKind.equals("GetMethod")){
            var leftType = table.getReturnType(left.get("value")).getName();
            boolean isLeftArray = table.getReturnType(left.get("value")).isArray();
            if(!leftType.equals("boolean") || isLeftArray){
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + leftType + " and " + "boolean",
                        null
                ));
            }

        }
    }

    private Void varDecl(JmmNode node, SymbolTable table) {
        var name = node.get("name");
        var returntype = node.getParent().getChild(0);
        var checkArray = node.getChild(0).get("isArray");
        var value = node.getChild(0).get("value");
        var assignStmts = node.getParent().getChildren(Kind.I_D_ASSIGN_STMT);
        for (var stmt : assignStmts) {
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
                    if (c.getKind().equals(firstExpr.getKind())) {
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
            else if (Objects.equals(stmtname, name) && Objects.equals(stmt.getChild(0).isInstance(I_D_EXPR), true)){
                var idname = id.get("name");
                var assigns = node.getParent().getChildren(Kind.VAR_DECL);
                for (var assign : assigns){
                    if( Objects.equals(idname, assign.get("name"))){
                        var assignType = assign.getChild(0);
                        var imports = table.getImports();
                        boolean varIsImported = false;
                        boolean assignIsImported = false;
                        for( var imp : imports){
                            if(Objects.equals(imp, assignType.get("value"))) {
                                assignIsImported = true;
                            }
                            if(Objects.equals(imp, node.getChild(0).get("value"))){
                                varIsImported = true;
                            }
                        }
                        if(varIsImported && assignIsImported){
                            continue;
                        }
                        if((Objects.equals(assignType.get("value"), table.getClassName()) && Objects.equals(node.getChild(0).get("value"), table.getSuper())) || (Objects.equals(assignType.get("value"), table.getSuper()) && Objects.equals(node.getChild(0).get("value"), table.getClassName()))){
                            continue;
                        }
                        if(!Objects.equals(assignType, node.getChild(0))){
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(node),
                                    NodeUtils.getColumn(node),
                                    "Incompatible types: " + assignType + " and " + node.getChild(0),
                                    null
                            ));
                        }
                    }
                }
            }
        }
        var whileStmts = node.getParent().getChildren(Kind.WHILE_STMT);
        for (var whileStmt : whileStmts) {
            var idExprs = whileStmt.getChildren(I_D_EXPR);
            for (var idexpr : idExprs) {
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
        return null;
    }

    private Void ifElseStmt(JmmNode node, SymbolTable table){
        var condition = node.getChild(0);
        if(condition.getKind().equals("BooleanExpr")){
            return null;
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                "Incompatible types: " + condition.getKind() + " and " + "boolean",
                null
        ));
        return null;
    }

    private Void returnStmt(JmmNode node, SymbolTable table) {
        var method = node.getParent();
        var returnType = table.getReturnType(method.get("name"));
        var returnExpr = node.getChild(0);
        if (returnType.getName().equals("int") && returnExpr.getKind().equals("IntegerExpr")) {
            return null;
        }
        if (returnType.getName().equals("boolean") && returnExpr.getKind().equals("BooleanExpr")) {
            return null;
        }
        if (returnExpr.getKind().equals("IDExpr")) {
            var idName = returnExpr.get("name");
            var locals = table.getLocalVariables(currentMethod);
            var params = table.getParameters(currentMethod);
            if (checkIncompatibleTypesIdExpr(node, returnType, idName, params)) return null;
            if (checkIncompatibleTypesIdExpr(node, returnType, idName, locals)) return null;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Incompatible types: " + idName + " and " + returnType.getName(),
                    null
            ));
        }
        if(returnExpr.getKind().equals("GetMethod")){
            var methodName = returnExpr.get("value");
            var methodReturnType = table.getReturnType(methodName);
            if(Objects.equals(methodReturnType.getName(), returnType.getName()) && Objects.equals(methodReturnType.isArray(), returnType.isArray())){
                return null;
            }
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Incompatible types: " + methodName + " and " + returnType.getName(),
                    null
            ));
        }
        if(returnExpr.getKind().equals("ArrayExpr")){
            var child = returnExpr.getChild(1);
            if(child.getKind().equals("IntegerExpr")){
                if (Objects.equals(returnType.getName(), "int") && Objects.equals(returnType.isArray(), false)) {
                    return null;
                }
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + returnExpr.getKind() + " and " + returnType.getName(),
                        null
                ));
            }
        }
        if(returnExpr.getKind().equals("BinaryExpr") && returnType.getName().equals("int") && !returnType.isArray()){
            return null;
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                "Incompatible types: " + returnExpr.getKind() + " and " + returnType.getName(),
                null
        ));
    return null;
    }

    private boolean checkIncompatibleTypesIdExpr(JmmNode node, Type returnType, String idName, List<Symbol> params) {
        for (var param : params) {
            if (Objects.equals(param.getName(), idName)){
                if (Objects.equals(param.getType().getName(), returnType.getName()) && Objects.equals(param.getType().isArray(), returnType.isArray())) {
                    return true;
                }
                else {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Incompatible types: " + idName + " and " + returnType.getName(),
                            null
                    ));
                    return false;
                }
            }
        }
        return true;
    }

    private Void binExpr(JmmNode node, SymbolTable table) {
        var left = node.getChild(0);
        var right = node.getChild(1);
        var leftKind = left.getKind();
        var rightKind = right.getKind();
        var locals = table.getLocalVariables(currentMethod);
        var params = table.getParameters(currentMethod);
        checkBinExprTypes(node, leftKind, rightKind, params);
        checkBinExprTypes(node, leftKind, rightKind, locals);
        return null;
    }

    private void checkBinExprTypes(JmmNode node, String leftKind, String rightKind, List<Symbol> locals) {
        if (!locals.isEmpty() && locals.size() >= 2) {
            var leftType = locals.get(0).getType().getName();
            var rightType = locals.get(1).getType().getName();
            var leftArray = locals.get(0).getType().isArray();
            var rightArray = locals.get(1).getType().isArray();
            if (!leftType.equals(rightType)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + leftType + " and " + rightType,
                        null
                ));
            }
            else if (leftArray != rightArray) {
                String arrayType = leftArray ? leftType : "array";
                String nonArrayType = leftArray ? "array" : leftType;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + arrayType + " and " + nonArrayType,
                        null
                ));
            }
        }
        for (var local : locals) {
            if ((leftKind.equals("IntegerExpr") || rightKind.equals("IntegerExpr")) && (!Objects.equals(local.getType().getName(), "int"))) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + "int" + " and " + local.getType().getName(),
                        null
                ));
            }

        }
    }

    private boolean checkOperandTypes(JmmNode node, SymbolTable table, JmmNode right, String rightKind, String leftType) {
        if (rightKind.equals("GetMethod")) {
            var rightType = table.getReturnType(right.get("value")).getName();
            if(Objects.equals(leftType, rightType)){
                return true;
            }
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Incompatible types: " + leftType + " and " + rightType,
                    null
            ));
        }
        return false;
    }

    private Void arrayExpr(JmmNode node, SymbolTable table) {
        var locals = table.getLocalVariables(currentMethod);
        var parent = node.getParent();
        var varDecls = parent.getParent().getChildren(Kind.VAR_DECL);
        for (var varDecl : varDecls) {
            var checkArray = varDecl.getChild(0).get("isArray");
            System.out.println(checkArray);
            if (Objects.equals(checkArray, "false")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Can't do an array expression with non array: " + node.getChildren(Kind.TYPE),
                        null
                ));

            }
        }
        var idexprs = node.getChildren(I_D_EXPR);
        for (var idexpr : idexprs) {
            var idname = idexpr.get("name");
            for (var local : locals) {
                if (Objects.equals(local.getName(), idname) && !Objects.equals(local.getType().getName(), "int")) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Can't do an array expression with non int: " + node.getChildren(Kind.TYPE),
                            null
                    ));
                }
            }
        }
        return null;
    }

    private Void binTypes(JmmNode node, SymbolTable table) {

        var left = node.getChild(0);
        var right = node.getChild(1);
        var locals = table.getLocalVariables(currentMethod);
        var params = table.getParameters(currentMethod);
        for (var param : params) {
            if (Objects.equals(param.getName(), left.get("name")) && Objects.equals(param.getType().getName(), "int") && right.getKind().equals("ThisExpr")) {
                var methods = table.getMethods();
                if (methods.contains(node.getParent().get("value"))) {
                    var retType = table.getReturnType(node.getParent().get("value"));
                    if (retType.getName().equals("int")) {
                        return null;
                    }
                    else {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(node),
                                NodeUtils.getColumn(node),
                                "Incompatible types: " + "int" + " and " + retType,
                                null
                        ));

                    }
                }
                return null;
            }
        }
        String leftKind = left.getKind();
        var rightKind = right.getKind();
        if(leftKind.equals("IntegerExpr") || rightKind.equals("IntegerExpr")){
            return null;
        } else if (leftKind.equals("IDExpr")){
            String leftType = "";
            for(var local : locals){
                if(Objects.equals(local.getName(), left.get("name"))){
                    leftType = local.getType().getName();
                    break;
                }
            }
            if(leftType.isEmpty()) {
                for (var param : params) {
                    if (Objects.equals(param.getName(), left.get("name"))) {
                        leftType = param.getType().getName();
                        break;
                    }
                }
                if(leftType.isEmpty()){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Variable not defined: " + left.get("name"),
                            null
                    ));
                }
            }
            if (rightKind.equals("IDExpr")){
                String rightType = "";
                for(var local : locals){
                    if(Objects.equals(local.getName(), right.get("name"))){
                        rightType = local.getType().getName();
                    }
                }
                if(rightType.isEmpty()) {
                    for (var param : params) {
                        if (Objects.equals(param.getName(), right.get("name"))) {
                            rightType = param.getType().getName();
                        }
                    }
                    if(rightType.isEmpty()){
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(node),
                                NodeUtils.getColumn(node),
                                "Variable not defined: " + right.get("name"),
                                null
                        ));
                    }
                }
                if(Objects.equals(leftType, rightType)){
                    return null;
                }
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        "Incompatible types: " + leftType + " and " + rightType,
                        null
                ));
            }
            else if (checkOperandTypes(node, table, right, rightKind, leftType)) return null;

        } else {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    "Incompatible types: " + leftKind + " and " + rightKind,
                    null
            ));
        }
        return null;
    }

    private Void listTypes(JmmNode node, SymbolTable table) {
        currentMethod = node.get("name");
        if (currentMethod.equals("varargs")) {
            var params = table.getParameters(currentMethod);
            for (var param : params) {
                for (var param2 : params) {
                    if (!Objects.equals(param.getName(), param2.getName()) && !Objects.equals(param.getType().getName(), param2.getType().getName())) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(node),
                                NodeUtils.getColumn(node),
                                "Different types: " + param.getType().getName() + " and " + param2.getType().getName(),
                                null
                        ));
                    }
                }
            }
        }

        var params = table.getParameters(currentMethod);
        // check for duplicated parameters
        for (int i = 0; i < params.size(); i++) {
            for (int j = i + 1; j < params.size(); j++) {
                if (Objects.equals(params.get(i).getName(), params.get(j).getName())) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Duplicated parameters: " + params.get(i).getName() + " and " + params.get(j).getName(),
                            null
                    ));
                }
            }
        }
        var locals = table.getLocalVariables(currentMethod);
        // check for duplicated local variables
        for (int i = 0; i < locals.size(); i++) {
            for (int j = i + 1; j < locals.size(); j++) {
                if (Objects.equals(locals.get(i).getName(), locals.get(j).getName())) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(node),
                            NodeUtils.getColumn(node),
                            "Duplicated local variables: " + locals.get(i).getName() + " and " + locals.get(j).getName(),
                            null
                    ));
                }
            }
        }

        return null;
    }

    public Void getMethod(JmmNode node, SymbolTable table) {
        var methodName = node.get("value");
        var methods = table.getMethods();
        var varNames = node.getChildren(I_D_EXPR);
        if (!varNames.isEmpty()) {
            var varName = varNames.get(0).get("name");
            for (var imp : table.getImports()) {
                if (varName.equals(imp)) {
                    return null;
                }
                for (var field : table.getFields()) {
                    if (Objects.equals(field.getName(), varName) && Objects.equals(field.getType().getName(), imp))
                        return null;
                }
                for (var param : table.getParameters(currentMethod)) {
                    if (Objects.equals(param.getName(), varName) && Objects.equals(param.getType().getName(), imp))
                        return null;
                }
                for (var local : table.getLocalVariables(currentMethod)) {
                    if (Objects.equals(local.getName(), varName) && Objects.equals(local.getType().getName(), imp))
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
        }

        var locals = table.getLocalVariables(currentMethod);
        if (methods.contains(methodName)) {
            var methodParams = table.getParameters(methodName);
            var args = node.getChildren(I_D_EXPR);
            for (Symbol param : methodParams) {
                var paramType = param.getType().getName();
                if (!args.isEmpty()) {
                    var arg = args.get(args.size() - 1);
                    for (Symbol local : locals) {
                        if (Objects.equals(local.getName(), arg.get("name"))) {
                            if (!Objects.equals(local.getType().getName(), paramType)) {
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        NodeUtils.getLine(node),
                                        NodeUtils.getColumn(node),
                                        "Incompatible types: " + local.getType().getName() + " and " + paramType,
                                        null
                                ));
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
