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
                        .map(importDecl -> importDecl.get("value"))
                        .toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {

        List<Symbol> fields = new ArrayList<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
            if (!method.getChildren().isEmpty()) {
                var type = method.getChild(0);
                for (var param : method.getChildren(TYPE)) {
                    fields.add(new Symbol(new Type(param.get("value"), false), type.getChild(0).get("value")));
                }
            }
        }

        return fields;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
            if (!method.getChildren().isEmpty()) {
                var type = method.getChild(0);
                map.put(method.get("name"), new Type(type.getChild(0).get("value"), false));
            }
            else {
                map.put("main", new Type("static void", false));
            }
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();
        List<Symbol> params = new ArrayList<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
            if (!method.getChildren().isEmpty()) {
                var type = method.getChild(0);
                for (var param : method.getChildren(TYPE)) {
                    params.add(new Symbol(new Type(param.get("value"), false), type.getChild(0).get("value")));
                    map.put(type.getChild(0).get("value"), params);
                }
                var returnType = type.getChild(0).get("value");
                map.computeIfAbsent(returnType, k -> new ArrayList<>());
            }
        }

        return map;
    }


    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        var child = classDecl.getChildren(METHOD_DECL);
        for (var method : child) {
            if (!method.getChildren().isEmpty()) {
                var type = method.getChild(0);
                var locals = new ArrayList<Symbol>();
                for (var local : method.getChildren(Kind.VAR_DECL)) {
                    locals.add(new Symbol(new Type(local.getChild(0).get("value"), false), method.get("name")));
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
