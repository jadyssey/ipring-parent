package org.ipring.jacg.util;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JavaMethodCallAnalysisUtils {

    private JavaMethodCallAnalysisUtils() {
    }

    public static ImportContext buildImportContext(Iterable<ImportDeclaration> imports) {
        Map<String, String> explicitImports = new HashMap<>();
        List<String> onDemandImports = new ArrayList<>();
        for (ImportDeclaration imp : imports) {
            if (imp.isStatic()) {
                continue;
            }
            String imported = JavaParseTextUtils.normalizeInlineWhitespace(imp.getNameAsString());
            if (imported.isEmpty()) {
                continue;
            }
            if (imp.isAsterisk()) {
                onDemandImports.add(imported);
            } else {
                explicitImports.put(JavaParseTextUtils.shortClassName(imported), imported);
            }
        }
        return new ImportContext(explicitImports, onDemandImports);
    }

    public static Map<String, String> buildLocalVarTypeMap(MethodDeclaration method) {
        Map<String, String> map = new HashMap<>();
        method.getParameters().forEach(param ->
                map.put(param.getNameAsString(), JavaParseTextUtils.normalizeTypeName(param.getType().asString()))
        );
        method.findAll(VariableDeclarator.class).forEach(var ->
                map.putIfAbsent(var.getNameAsString(), JavaParseTextUtils.normalizeTypeName(var.getType().asString()))
        );
        return map;
    }

    public static String inferScopeType(
            String scopeExpr,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldTypeMap
    ) {
        String scopeVar = JavaParseTextUtils.normalizeScopeVar(scopeExpr);
        String localType = JavaParseTextUtils.normalizeTypeName(localVarTypeMap.get(scopeVar));
        if (localType != null && !localType.isEmpty()) {
            return localType;
        }

        String fieldType = JavaParseTextUtils.normalizeTypeName(fieldTypeMap.get(scopeVar));
        if (fieldType != null && !fieldType.isEmpty()) {
            return fieldType;
        }

        if (JavaParseTextUtils.isTypeLikeScope(scopeExpr)) {
            return JavaParseTextUtils.normalizeTypeName(scopeExpr);
        }
        return null;
    }

    public static String relativizePath(Path projectRoot, Path path) {
        try {
            return JavaParseTextUtils.normalizePathSlash(projectRoot.relativize(path.toAbsolutePath().normalize()).toString());
        } catch (Exception ex) {
            return JavaParseTextUtils.normalizePathSlash(path.toString());
        }
    }

    public static final class ImportContext {
        private final Map<String, String> explicitImports;
        private final List<String> onDemandImports;

        public ImportContext(Map<String, String> explicitImports, List<String> onDemandImports) {
            this.explicitImports = explicitImports;
            this.onDemandImports = onDemandImports;
        }

        public Map<String, String> getExplicitImports() {
            return explicitImports;
        }

        public List<String> getOnDemandImports() {
            return onDemandImports;
        }
    }
}
