package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        System.out.println(ollirResult.getOllirCode().toString());

        try {
            ollirResult.getOllirClass().checkMethodLabels();
            ollirResult.getOllirClass().buildCFGs();
            ollirResult.getOllirClass().buildVarTables();
        } catch (OllirErrorException e) {
            throw new RuntimeException(e);
        }
        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CondBranchInstruction.class, this::generateBranch);
        generators.put(GotoInstruction.class, this::generateGoto);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        var classModifier = classUnit.getClassAccessModifier() != AccessModifier.DEFAULT ?
                classUnit.getClassName().toLowerCase() + " " :
                "";

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ")
                .append(classModifier)
                .append(className)
                .append(NL);

        if (classUnit.getSuperClass() != null) {
            code.append(".super ")
                    .append(classUnit.getSuperClass())
                    .append("\n");
        } else {
            code.append(".super java/lang/Object\n");
        }

        for (Field field : classUnit.getFields()) {
            String modifier = "";

            if(!field.getFieldAccessModifier().equals(AccessModifier.DEFAULT)) modifier = field.getFieldAccessModifier().name().toLowerCase() + " ";

            code.append(".field ")
                    .append(modifier)
                    .append(field.getFieldName())
                    .append(" ")
                    .append(myGetType(field.getFieldType()))
                    .append("\n");
        }

        var methods = new StringBuilder();
        String construct = "";

        for (var method : ollirResult.getOllirClass().getMethods()) {
            methods.append(generators.apply(method));
        }
        code.append(methods);
        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();


        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";
        if(method.isConstructMethod()){
            String superClass;
            if(ollirResult.getOllirClass().getSuperClass() != null){
                superClass = ollirResult.getOllirClass().getSuperClass();
            }else superClass = "java/lang/Object";

            var defaultConstructor = "\n.method public <init>()V\n   aload_0\n   invokespecial "
                    + superClass +
                    "/<init>()V\n   return\n.end method\n";
            code.append(defaultConstructor);
        }else {
            code.append("\n.method ")
                    .append(modifier);

            if (method.isStaticMethod()) {
                code.append("static ");
            } else if (method.isFinalMethod()) {
                code.append("final ");
            }

            var methodName = method.getMethodName();

            var methodTypes = new StringBuilder();

            methodTypes.append("(");

            for (var param : method.getParams()) {
                methodTypes.append(myGetType(param.getType()));
            }

            methodTypes.append(")").append(myGetType(method.getReturnType()));

            code.append(methodName).append(methodTypes.toString()).append(NL);
            // Add limits

            var instCode = new StringBuilder();

            for (var inst : method.getInstructions()) {
                instCode.append(StringLines.getLines(generators.apply(inst)).stream()
                        .collect(Collectors.joining(NL + TAB, TAB, NL)));
                if((inst instanceof CallInstruction) && (((CallInstruction) inst).getReturnType().getTypeOfElement() == ElementType.VOID) && ((CallInstruction) inst).getInvocationType().equals(CallType.invokespecial)){
                    instCode.append(TAB).append("pop").append(NL);
                }
            }
            int localCount = method.getVarTable().size();
            if(!method.isStaticMethod()) localCount++;

            System.out.println("local: " + localCount);

            code.append(TAB).append(".limit stack 99").append(NL);
            code.append(TAB).append(".limit locals ").append(localCount).append(NL);

            code.append(instCode);
            code.append(".end method\n");
        }
        // unset method
        currentMethod = null;
        return code.toString();
    }

    private String myGetType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        String stringBuilder = "";
        if (elementType == ElementType.ARRAYREF) {
            stringBuilder += "[";
            elementType = ((ArrayType) type).getElementType().getTypeOfElement();
        }
        switch (elementType) {
            case INT32 -> stringBuilder += "I";
            case BOOLEAN -> stringBuilder += "Z";
            case STRING -> stringBuilder += "Ljava/lang/String;";
            case VOID -> stringBuilder += "V";
            case OBJECTREF -> {
                return stringBuilder;
            }
            default -> {
                return stringBuilder;
            }
        }

        return stringBuilder;
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();
        // generate code for loading what's on the right
        var rhs = assign.getRhs();
        code.append(generators.apply(rhs));
        // store value in the stack in destination
        //if ((assign.getRhs() instanceof CallInstruction)) return code.toString();
        var lhs = assign.getDest();
        var operand = (Operand) lhs;
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        String vreg;
        if(reg > 3) vreg = " " + reg + NL; else vreg = "_" + reg + NL;
        var ret = switch (operand.getType().getTypeOfElement()) {
            case INT32 -> "istore";
            case BOOLEAN -> "istore";
            case OBJECTREF -> "astore";
            case ARRAYREF -> "astore";
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        };

        code.append(ret).append(vreg);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        String sliteral = literal.getLiteral();

        if (Integer.parseInt(sliteral) < -1 || Integer.parseInt(sliteral) > 5) {
            if (Integer.parseInt(sliteral) < -128 || Integer.parseInt(sliteral) > 127) {
                if (Integer.parseInt(sliteral) < -32768 || Integer.parseInt(sliteral) > 32767) {
                    return "ldc " + sliteral + NL;
                } else {
                    return "sipush " + sliteral + NL;
                }
            } else {
                return "bipush " + sliteral + NL;
            }
        } else {
            return "iconst_" + sliteral + NL;
        }
    }

    private String generateOperand(Operand operand) {
        // get register
        var code = new StringBuilder();
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        String vreg;
        if(reg > 3) vreg = " " + reg + NL; else vreg = "_" + reg + NL;

        switch (operand.getType().getTypeOfElement()) {
            case INT32 -> {
                code.append("iload").append(vreg);
            }
            case BOOLEAN -> {
                code.append("iload").append(vreg);
            }
            case OBJECTREF -> {
                code.append("aload").append(vreg);
            }
            case ARRAYREF -> {
                code.append("aload").append(vreg);
            }
            case THIS -> {
                return "aload_0" + NL;
            }
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        }
        return code.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case AND -> "iand";
            case LTH -> "icmp";
            default -> "";
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        ElementType type = returnInst.getReturnType().getTypeOfElement();

        var ret = switch (type) {
            case VOID -> "return";
            case INT32 -> "ireturn";
            case BOOLEAN -> "ireturn";
            case OBJECTREF -> "areturn";
            case ARRAYREF -> "areturn";
            default -> throw new NotImplementedException(type);
        };

        if (!type.equals(ElementType.VOID))
            code.append(generators.apply(returnInst.getOperand()));
        code.append(ret).append(NL);


        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var call = new StringBuilder();
        CallType callType = callInstruction.getInvocationType();
        String methodName = "";
        boolean isInvoke = !(callType.equals(CallType.NEW) || callType.equals(CallType.arraylength));

        if(isInvoke){
            methodName = (((LiteralElement) callInstruction.getMethodName()).getLiteral()).replace("\"", "");
        }

        switch (callType) {
            case invokestatic: {
                String className = ((Operand) callInstruction.getOperands().get(0)).getName();
                call.append("invokestatic ").append(getObjClass(className)).append("/").append(methodName);
                break;
            }
            case invokespecial: {
                String superClass = ((ClassType) callInstruction.getOperands().get(0).getType()).getName();
                call.append("invokespecial ").append(getObjClass(superClass)).append("/").append(methodName);
                break;
            }
            case invokevirtual: {
                String objectRef = ((ClassType) callInstruction.getOperands().get(0).getType()).getName();
                call.append("invokevirtual ").append(getObjClass(objectRef)).append("/").append(methodName);
                break;
            }
            case NEW: {
                if (callInstruction.getOperands().get(0).getType().getTypeOfElement().equals(ElementType.OBJECTREF)) {
                    String className = this.getObjClass(((Operand)callInstruction.getOperands().get(0)).getName());
                    call.append("new ").append(className).append("\ndup\n");
                    break;
                } else if (callInstruction.getOperands().get(0).getType().getTypeOfElement().equals(ElementType.ARRAYREF)){
                    call.append(generators.apply(callInstruction.getOperands().get(1))) .append("newarray int\n");
                    break;
                }

            }
            case arraylength: {
                System.out.println(callInstruction.getOperands().get(0).getType().getTypeOfElement());
                call.append(generators.apply(callInstruction.getOperands().get(0))).append("arraylenght\n");
                break;
            }
        }

        if(isInvoke) {
            StringBuilder param = new StringBuilder();
            for (var operand : callInstruction.getOperands()) {
                Boolean cond1 = operand.equals(callInstruction.getOperands().get(0)) && callType.equals(CallType.invokestatic);
                Boolean cond2 = operand.equals(callInstruction.getOperands().get(1));
                if(cond1 || cond2) continue;
                code.append(generators.apply(operand));
                param.append(myGetType(operand.getType()));
            }

            call.append("(")
                    .append(param)
                    .append(")")
                    .append(myGetType(callInstruction.getReturnType())).append(NL);
        }
        return code.append(call.toString()).toString();
    }

    private String getObjClass (String className){
        for(String _imp : ollirResult.getOllirClass().getImports()){
            if(_imp.endsWith("." + className)){
                return _imp.replaceAll("\\.", "/");
            }
        }
        return className;
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction){
        StringBuilder code = new StringBuilder();

        Operand object = putFieldInstruction.getObject();
        Operand field = putFieldInstruction.getField();
        Element value = putFieldInstruction.getValue();

        code.append(generators.apply(object));
        code.append(generators.apply(value));

        code.append("putfield ")
                .append(ollirResult.getOllirClass().getClassName()).append("/")
                .append(field.getName()).append(" ")
                .append(myGetType(field.getType())).append(NL);
        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction){
        StringBuilder code = new StringBuilder();

        Operand object = getFieldInstruction.getObject();
        Operand field = getFieldInstruction.getField();

        code.append(generators.apply(object));

        code.append("getfield ")
                .append(ollirResult.getOllirClass().getClassName()).append("/")
                .append(field.getName()).append(" ")
                .append(myGetType(field.getType())).append(NL);
        return code.toString();
    }

    private String generateBranch(CondBranchInstruction condBranchInstruction){
        var code = new StringBuilder();
        code.append(generators.apply(condBranchInstruction.getCondition()));






        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInstruction){
        return "goto " + gotoInstruction.getLabel() + NL;
    }
}



