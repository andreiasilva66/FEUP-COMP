package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        System.out.println(root.toTree());

        var classDecl = root.getChildren().get(root.getChildren().size() - 1);

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("className");
        String superClass = null;

        if(classDecl.hasAttribute("superClassName")){
            superClass = classDecl.get("superClassName");
        }

        List<Symbol> fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var imports = buildImports(root);
        var returnTypes = buildReturnTypes(classDecl);
        Map<String, List<Symbol>> params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, superClass, fields, methods, imports, returnTypes, params, locals);
    }

    private static List<String> buildImports(JmmNode classDecl) {
        return classDecl.getChildren(Kind.IMPORT_DECL).stream()
                        .map(importDecl -> importDecl.get("ID"))
                        .toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {

        List<Symbol> fields = new ArrayList<>();
        var varDecl = classDecl.getChildren(VAR_DECL);
        for (var field : varDecl) {
            if(field.getChild(0).get("isVarargs").equals("true")){
                fields.add(new Symbol(new Type("int...", true), field.get("name")));
            }
            else{
                fields.add(new Symbol(new Type(field.getChild(0).get("value"), Objects.equals(field.getChild(0).get("isArray"),"true")), field.get("name")));
            }
        }

        return fields;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
                var type = method.getChild(0);
                if(type.getChild(0).get("isVarargs").equals("true")){
                    map.put(method.get("name"), new Type("int...", true));
                }
                else if (type.getChild(0).get("isArray").equals("true")) {
                    boolean value = true;
                    map.put(method.get("name"), new Type(type.getChild(0).get("value"), value));
                }
                else {
                    boolean value = false;
                    map.put(method.get("name"), new Type(type.getChild(0).get("value"), value));
                }
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
            List<Symbol> params = new ArrayList<>();
            if (!method.getChildren().isEmpty()) {
                var methParams = method.getChildren(PARAM);
                for (var param : methParams) {
                    if(param.getChild(0).get("isVarargs").equals("true")){
                        params.add(new Symbol(new Type("int...", true), param.get("name")));
                    }
                    else {
                        params.add(new Symbol(new Type(param.getChild(0).get("value"), Objects.equals(param.getChild(0).get("isArray"), "true")), param.get("name")));
                    }
                }
                map.put(method.get("name"), params);
            }
        }

        return map;
    }


    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
            if (!method.getChildren().isEmpty()) {
                var locals = new ArrayList<Symbol>();
                for (var local : method.getChildren(Kind.VAR_DECL)) {
                    if(local.getChild(0).get("isVarargs").equals("true")){
                        locals.add(new Symbol(new Type("int...", true), local.get("name")));
                    }
                    else{
                        locals.add(new Symbol(new Type(local.getChild(0).get("value"), local.getChild(0).get("isArray").equals("true")), local.get("name")));
                    }
                }
                map.put(method.get("name"), locals);
            }
        }

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for (var child : classDecl.getChildren(METHOD_DECL)) {
            methods.add(child.get("name"));
        }
        return methods;
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        List<Symbol> locals = new ArrayList<>();

        return locals;
    }

}
