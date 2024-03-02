package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getChildren().get(root.getChildren().size() - 1);

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");
        String superClass = null;

        if(classDecl.hasAttribute("superclassname")){
            superClass = classDecl.get("superclassname");
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

        return classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> {

                    var type = varDecl.getChild(0);

                    return new Symbol(new Type(type.get("name"), false), varDecl.get("name"));
                })
                .toList();
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        System.out.println(classDecl.toTree());

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    if(method.getChildren().size() > 0){
                        if(method.getChild(0).get("name") == "int[]"){
                            map.put(method.get("name"), new Type(method.getChild(0).get("name"), true));
                        }
                        else{
                            map.put(method.get("name"), new Type(method.getChild(0).get("name"), false));
                        }
                    }
                }
                );

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), method.getChildren(VAR_DECL).stream()
                                .map(varDecl -> {

                                    var type = varDecl.getChild(0);

                                    return new Symbol(new Type(type.get("name"), false), varDecl.get("name"));
                                }).toList()));

        return map;
    }


    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
