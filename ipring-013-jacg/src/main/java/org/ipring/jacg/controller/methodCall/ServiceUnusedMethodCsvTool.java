package org.ipring.jacg.controller.methodCall;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import lombok.Data;
import org.ipring.jacg.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.ipring.jacg.util.JavaParseTextUtils.*;


/**
 * Scan project and output unused methods in service interfaces to CSV.
 * Rule: interface && simple name contains "service" (case-insensitive).
 */
public class ServiceUnusedMethodCsvTool {

    private static final String JAVA_SUFFIX = ".java";
    private static final int SCAN_PROGRESS_INTERVAL = 200;
    private static final Set<String> EXCLUDED_SERVICE_FULL_NAMES = new HashSet<>(Arrays.asList(
            "com.cds.base.service.AuthApiService"
    ));

    private static final Map<String, ClassContext> classContextMap = new HashMap<>();
    private static final Map<MethodRef, ServiceMethodInfo> serviceMethodInfoMap = new HashMap<>();
    private static final Set<String> serviceInterfaceSet = new LinkedHashSet<>();
    private static final Map<String, Set<String>> serviceSimpleToFullMap = new HashMap<>();
    private static final Set<MethodRef> referencedServiceMethods = new LinkedHashSet<>();

    private static int scannedJavaFileCount = 0;
    private static int scannedJavaRootCount = 0;
    private static long scanStartMillis = 0L;

    @Data
    static class RuntimeConfig {
        String projectRoot;
        String outputCsv;

        static RuntimeConfig of(String projectRoot, String outputCsv) {
            RuntimeConfig config = new RuntimeConfig();
            config.setProjectRoot(projectRoot);
            config.setOutputCsv(outputCsv);
            return config;
        }
    }

    static class MethodRef {
        final String serviceFullName;
        final String methodName;
        final int paramCount;

        MethodRef(String serviceFullName, String methodName, int paramCount) {
            this.serviceFullName = serviceFullName;
            this.methodName = methodName;
            this.paramCount = paramCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodRef)) {
                return false;
            }
            MethodRef other = (MethodRef) obj;
            return paramCount == other.paramCount
                    && methodName.equals(other.methodName)
                    && serviceFullName.equals(other.serviceFullName);
        }

        @Override
        public int hashCode() {
            int result = serviceFullName.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + paramCount;
            return result;
        }
    }

    static class ServiceMethodInfo {
        final MethodRef ref;
        final String sourceFile;
        final int lineNo;

        ServiceMethodInfo(MethodRef ref, String sourceFile, int lineNo) {
            this.ref = ref;
            this.sourceFile = sourceFile;
            this.lineNo = lineNo;
        }
    }

    static class MethodCallRef {
        final String ownerClass;
        final String targetTypeName;
        final String methodName;
        final int argCount;

        MethodCallRef(String ownerClass, String targetTypeName, String methodName, int argCount) {
            this.ownerClass = ownerClass;
            this.targetTypeName = targetTypeName;
            this.methodName = methodName;
            this.argCount = argCount;
        }
    }

    static class ClassContext {
        final String fullClassName;
        final String pkg;
        final Map<String, String> explicitImports;
        final List<String> onDemandImports;
        final List<MethodCallRef> methodCalls = new ArrayList<>();

        ClassContext(String fullClassName, String pkg, Map<String, String> explicitImports, List<String> onDemandImports) {
            this.fullClassName = fullClassName;
            this.pkg = pkg;
            this.explicitImports = explicitImports;
            this.onDemandImports = onDemandImports;
        }
    }

    public static void main(String[] args) throws Exception {
        RuntimeConfig config = RuntimeConfig.of("D:\\project\\dbu-mod-waybill", "unused_service_methods.csv");
        if (args != null && args.length > 0 && !JavaParseTextUtils.normalizeInlineWhitespace(args[0]).isEmpty()) {
            config.setProjectRoot(JavaParseTextUtils.normalizeInlineWhitespace(args[0]));
        }
        if (args != null && args.length > 1 && !JavaParseTextUtils.normalizeInlineWhitespace(args[1]).isEmpty()) {
            config.setOutputCsv(JavaParseTextUtils.normalizeInlineWhitespace(args[1]));
        }

        resetState();
        JavaParseLogUtils.logInfo("Tool start. projectRoot=" + config.projectRoot + ", outputCsv=" + config.outputCsv);

        scanProject(config.projectRoot);
        resolveReferences();
        List<ServiceMethodInfo> unusedMethods = collectUnusedMethods();
        writeCsv(config.projectRoot, config.outputCsv, unusedMethods);

        JavaParseLogUtils.logInfo(String.format(
                Locale.ROOT,
                "Done. javaRoots=%d, javaFiles=%d, serviceInterfaces=%d, serviceMethods=%d, referencedMethods=%d, unusedMethods=%d, elapsed=%dms",
                scannedJavaRootCount,
                scannedJavaFileCount,
                serviceInterfaceSet.size(),
                serviceMethodInfoMap.size(),
                referencedServiceMethods.size(),
                unusedMethods.size(),
                System.currentTimeMillis() - scanStartMillis
        ));
    }

    private static void resetState() {
        classContextMap.clear();
        serviceMethodInfoMap.clear();
        serviceInterfaceSet.clear();
        serviceSimpleToFullMap.clear();
        referencedServiceMethods.clear();
        scannedJavaFileCount = 0;
        scannedJavaRootCount = 0;
        scanStartMillis = System.currentTimeMillis();
    }

    private static void scanProject(String projectRoot) throws Exception {
        Path root = Paths.get(projectRoot).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Project path not found: " + root);
        }
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Project path is not a directory: " + root);
        }

        List<Path> javaRoots = JavaSourceScanUtils.findAllJavaRoots(root);
        if (javaRoots.isEmpty()) {
            JavaParseLogUtils.logWarn("No src/main/java found under: " + root);
            return;
        }

        for (Path javaRoot : javaRoots) {
            scannedJavaRootCount++;
            JavaParseLogUtils.logInfo("Scanning java root: " + javaRoot);
            scanJavaRoot(javaRoot, root);
        }
    }

    private static void scanJavaRoot(Path javaRoot, Path projectRoot) throws Exception {
        List<Path> javaFiles = JavaSourceScanUtils.collectJavaFiles(javaRoot);

        for (Path javaFile : javaFiles) {
            scannedJavaFileCount++;
            logScanProgressIfNeeded();
            try {
                CompilationUnit cu = StaticJavaParser.parse(javaFile);
                processCompilationUnit(cu, javaFile, projectRoot);
            } catch (Exception ex) {
                JavaParseLogUtils.logWarn("Skip parse error: " + javaFile + " | " + JavaParseTextUtils.normalizeInlineWhitespace(ex.getMessage()));
            }
        }
    }

    private static void processCompilationUnit(CompilationUnit cu, Path javaFile, Path projectRoot) {
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        JavaMethodCallAnalysisUtils.ImportContext importContext = JavaMethodCallAnalysisUtils.buildImportContext(cu.getImports());
        Map<String, String> explicitImports = importContext.getExplicitImports();
        List<String> onDemandImports = importContext.getOnDemandImports();

        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = clazz.getNameAsString();
            String fullName = pkg.isEmpty() ? className : pkg + "." + className;

            ClassContext ctx = new ClassContext(fullName, pkg, new HashMap<>(explicitImports), new ArrayList<>(onDemandImports));
            classContextMap.put(fullName, ctx);

            Map<String, String> fieldTypeMap = new HashMap<>();
            clazz.getFields().forEach(field -> field.getVariables().forEach(var ->
                    fieldTypeMap.put(var.getNameAsString(), normalizeTypeName(var.getType().asString()))
            ));

            boolean serviceInterface = isTargetServiceInterface(clazz, fullName);
            if (serviceInterface) {
                registerServiceInterface(fullName);
                registerServiceMethods(clazz, fullName, projectRoot, javaFile);
            }

            clazz.getMethods().forEach(method -> collectMethodCalls(fullName, serviceInterface, method, fieldTypeMap, ctx));
        }
    }

    private static boolean isTargetServiceInterface(ClassOrInterfaceDeclaration clazz, String fullName) {
        if (!clazz.isInterface()) {
            return false;
        }
        if (EXCLUDED_SERVICE_FULL_NAMES.contains(fullName)) {
            return false;
        }
        String name = JavaParseTextUtils.normalizeInlineWhitespace(clazz.getNameAsString()).toLowerCase(Locale.ROOT);
        return name.contains("service");
    }

    private static void registerServiceInterface(String serviceFullName) {
        serviceInterfaceSet.add(serviceFullName);
        serviceSimpleToFullMap.computeIfAbsent(JavaParseTextUtils.shortClassName(serviceFullName), key -> new LinkedHashSet<>()).add(serviceFullName);
    }

    private static void registerServiceMethods(
            ClassOrInterfaceDeclaration clazz,
            String serviceFullName,
            Path projectRoot,
            Path javaFile
    ) {
        clazz.getMethods().forEach(method -> {
            if (method.isStatic()) {
                return;
            }
            MethodRef ref = new MethodRef(serviceFullName, method.getNameAsString(), method.getParameters().size());

            String relativePath = JavaMethodCallAnalysisUtils.relativizePath(projectRoot, javaFile);
            int lineNo = method.getBegin().map(pos -> pos.line).orElse(-1);
            serviceMethodInfoMap.put(ref, new ServiceMethodInfo(ref, relativePath, lineNo));
        });
    }

    private static void collectMethodCalls(
            String ownerClass,
            boolean ownerIsServiceInterface,
            MethodDeclaration method,
            Map<String, String> fieldTypeMap,
            ClassContext ctx
    ) {
        if (!method.getBody().isPresent()) {
            return;
        }

        Map<String, String> localVarTypeMap = JavaMethodCallAnalysisUtils.buildLocalVarTypeMap(method);
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String calledMethodName = call.getNameAsString();
            int argCount = call.getArguments().size();

            if (!call.getScope().isPresent()) {
                if (ownerIsServiceInterface) {
                    referencedServiceMethods.add(new MethodRef(ownerClass, calledMethodName, argCount));
                }
                return;
            }

            String scopeExpr = JavaParseTextUtils.normalizeInlineWhitespace(call.getScope().get().toString());
            String typeName = JavaMethodCallAnalysisUtils.inferScopeType(scopeExpr, localVarTypeMap, fieldTypeMap);
            if (typeName == null || typeName.isEmpty()) {
                return;
            }
            ctx.methodCalls.add(new MethodCallRef(ownerClass, typeName, calledMethodName, argCount));
        });

        method.findAll(MethodReferenceExpr.class).forEach(ref -> {
            String calledMethodName = ref.getIdentifier();
            String scopeExpr = clean(ref.getScope().toString());
            String scopeVar = normalizeScopeVar(scopeExpr);
            if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
                if (ownerIsServiceInterface) {
                    markServiceMethodReferenced(ownerClass, calledMethodName, -1);
                }
                return;
            }

            String typeName = inferScopeType(scopeExpr, localVarTypeMap, fieldTypeMap);
            if (typeName == null || typeName.isEmpty()) {
                return;
            }
            ctx.methodCalls.add(new MethodCallRef(ownerClass, typeName, calledMethodName, -1));
        });
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private static Map<String, String> buildLocalVarTypeMap(MethodDeclaration method) {
        Map<String, String> map = new HashMap<>();
        method.getParameters().forEach(param ->
                map.put(param.getNameAsString(), normalizeTypeName(param.getType().asString()))
        );
        method.findAll(VariableDeclarator.class).forEach(var ->
                map.putIfAbsent(var.getNameAsString(), normalizeTypeName(var.getType().asString()))
        );
        method.findAll(LambdaExpr.class).forEach(lambda ->
                lambda.getParameters().forEach(param -> {
                    String typeName = normalizeTypeName(param.getType().asString());
                    if (typeName == null || typeName.isEmpty() || "var".equalsIgnoreCase(typeName)) {
                        return;
                    }
                    map.putIfAbsent(param.getNameAsString(), typeName);
                })
        );
        return map;
    }

    private static String inferScopeType(
            String scopeExpr,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldTypeMap
    ) {
        String scopeVar = normalizeScopeVar(scopeExpr);
        String localType = normalizeTypeName(localVarTypeMap.get(scopeVar));
        if (localType != null && !localType.isEmpty()) {
            return localType;
        }

        String fieldType = normalizeTypeName(fieldTypeMap.get(scopeVar));
        if (fieldType != null && !fieldType.isEmpty()) {
            return fieldType;
        }

        if (isTypeLikeScope(scopeExpr)) {
            return normalizeTypeName(scopeExpr);
        }
        return null;
    }

    private static void resolveReferences() {
        for (ClassContext ctx : classContextMap.values()) {
            for (MethodCallRef call : ctx.methodCalls) {
                List<String> serviceCandidates = resolveServiceCandidates(call.targetTypeName, ctx);
                for (String serviceType : serviceCandidates) {
                    markServiceMethodReferenced(serviceType, call.methodName, call.argCount);
                }
            }
        }
    }

    private static void markServiceMethodReferenced(String serviceType, String methodName, int argCount) {
        if (argCount >= 0) {
            MethodRef ref = new MethodRef(serviceType, methodName, argCount);
            if (serviceMethodInfoMap.containsKey(ref)) {
                referencedServiceMethods.add(ref);
            }
            return;
        }

        for (MethodRef candidate : serviceMethodInfoMap.keySet()) {
            if (!candidate.serviceFullName.equals(serviceType)) {
                continue;
            }
            if (!candidate.methodName.equals(methodName)) {
                continue;
            }
            referencedServiceMethods.add(candidate);
        }
    }

    private static List<String> resolveServiceCandidates(String rawTypeName, ClassContext ctx) {
        String typeName = normalizeTypeName(rawTypeName);
        if (typeName == null || typeName.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (typeName.contains(".")) {
            if (serviceInterfaceSet.contains(typeName)) {
                result.add(typeName);
            }
            return new ArrayList<>(result);
        }

        String simpleName = JavaParseTextUtils.shortClassName(typeName);
        String imported = ctx.explicitImports.get(simpleName);
        if (imported != null && serviceInterfaceSet.contains(imported)) {
            result.add(imported);
        }

        if (ctx.pkg != null && !ctx.pkg.isEmpty()) {
            String samePackage = ctx.pkg + "." + simpleName;
            if (serviceInterfaceSet.contains(samePackage)) {
                result.add(samePackage);
            }
        }

        for (String pkg : ctx.onDemandImports) {
            String candidate = pkg + "." + simpleName;
            if (serviceInterfaceSet.contains(candidate)) {
                result.add(candidate);
            }
        }

        Set<String> fallback = serviceSimpleToFullMap.get(simpleName);
        if (fallback != null) {
            result.addAll(fallback);
        }
        return new ArrayList<>(result);
    }

    private static List<ServiceMethodInfo> collectUnusedMethods() {
        List<ServiceMethodInfo> unused = new ArrayList<>();
        for (Map.Entry<MethodRef, ServiceMethodInfo> entry : serviceMethodInfoMap.entrySet()) {
            if (!referencedServiceMethods.contains(entry.getKey())) {
                unused.add(entry.getValue());
            }
        }
        unused.sort(Comparator
                .comparing((ServiceMethodInfo item) -> item.ref.serviceFullName)
                .thenComparing(item -> item.ref.methodName)
                .thenComparingInt(item -> item.ref.paramCount)
                .thenComparing(item -> item.sourceFile)
                .thenComparingInt(item -> item.lineNo));
        return unused;
    }

    private static void writeCsv(String projectRoot, String outputCsv, List<ServiceMethodInfo> rows) throws Exception {
        Path outputPath = Paths.get(outputCsv).toAbsolutePath().normalize();

        String lineSeparator = System.lineSeparator();
        StringBuilder content = new StringBuilder(256 + rows.size() * 128);
        content.append("project_root,service_interface,method_name,param_count,source_file,line_number")
                .append(lineSeparator);
        for (ServiceMethodInfo row : rows) {
            content.append(CsvIOUtils.csvAlwaysQuote(projectRoot)).append(",")
                    .append(CsvIOUtils.csvAlwaysQuote(row.ref.serviceFullName)).append(",")
                    .append(CsvIOUtils.csvAlwaysQuote(row.ref.methodName)).append(",")
                    .append(row.ref.paramCount).append(",")
                    .append(CsvIOUtils.csvAlwaysQuote(row.sourceFile)).append(",")
                    .append(row.lineNo)
                    .append(lineSeparator);
        }

        CsvIOUtils.writeUtf8(outputPath, content.toString());
        JavaParseLogUtils.logInfo("CSV written: " + outputPath);
    }

    private static void logScanProgressIfNeeded() {
        if (scannedJavaFileCount <= 0 || scannedJavaFileCount % SCAN_PROGRESS_INTERVAL != 0) {
            return;
        }
        JavaParseLogUtils.logInfo(String.format(
                Locale.ROOT,
                "Scan progress: javaFiles=%d, serviceInterfaces=%d, serviceMethods=%d, references=%d",
                scannedJavaFileCount,
                serviceInterfaceSet.size(),
                serviceMethodInfoMap.size(),
                referencedServiceMethods.size()
        ));
    }

}
