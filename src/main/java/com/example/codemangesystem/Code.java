package com.example.codemangesystem;

import java.util.List;
import java.util.Map;

public class Code {
    private final String fileName;
    private final String packageName;
    private final List<String> imports;
    private final List<String> annotations;
    private final List<String> comments;
    private final Map<String, String> functions;
    private final String variables;

    public Code(String fileName, String packageName, List<String> imports, List<String> annotations,
                List<String> comments, Map<String, String> functions, String variables) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.imports = imports;
        this.annotations = annotations;
        this.comments = comments;
        this.functions = functions;
        this.variables = variables;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public List<String> getComments() {
        return comments;
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public String getVariables() {
        return variables;
    }
}
