package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {

    private final List<String> importedClasses;
    private final String className;
    private final String superClassName;
    private final List<Symbol> fields;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;

    public JmmSymbolTable(List<String> importedClasses,
                          String className,
                          String superClassName,
                          List<Symbol> fields,
                          List<String> methods, //
                          Map<String, Type> returnTypes, //
                          Map<String, List<Symbol>> params, //
                          Map<String, List<Symbol>> locals) { //
        this.importedClasses = importedClasses;
        this.className = className;
        this.superClassName = superClassName;
        this.fields = fields;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
    }

    @Override
    public List<String> getImports() {
        return Collections.unmodifiableList(importedClasses);
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return Collections.unmodifiableList(fields);
        // throw new NotImplementedException();
    }

    @Override
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        // TODO: Simple implementation that needs to be expanded
        return new Type(TypeUtils.getIntTypeName(), false);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(params.get(methodSignature));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(locals.get(methodSignature));
    }

}
