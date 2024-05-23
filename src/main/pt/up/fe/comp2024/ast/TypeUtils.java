package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String OBJECT_TYPE_NAME = "Object";
    private static final String PARAMETERS_TYPE_NAME = "Parameters";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    public static String getObjectType() {
        return OBJECT_TYPE_NAME;
    }

    public static String getParametersType() {
        return PARAMETERS_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case I_D_EXPR -> getVarExprType(expr, table);
            case INTEGER_EXPR -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_EXPR -> new Type("boolean", false);
            case GET_METHOD -> table.getReturnType(expr.get("name"));
            case BINARY_BOOL_EXPR -> new Type("boolean", false);

            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "/", "*" -> new Type(INT_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        var name = varRefExpr.get("name");
        JmmNode parent = varRefExpr.getParent();
        while(!parent.getKind().equals("MethodDecl")){
            parent = parent.getParent();
        }
        var methodName = parent.get("name");
        for(var local : table.getLocalVariables(methodName)){
            if(local.getName().equals(name)){
                return new Type(local.getType().getName(), local.getType().isArray());
            }
        }
        for(var param : table.getParameters(methodName)){
            if(param.getName().equals(name)){
                return new Type(param.getType().getName(), param.getType().isArray());
            }
        }
        for(var field : table.getFields()){
            if(field.getName().equals(name)){
                return new Type(field.getType().getName(), field.getType().isArray());
            }
        }
        return new Type(INT_TYPE_NAME, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
