package org.ipring.jacg.controller.methodCall;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import lombok.Data;
import org.ipring.jacg.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MapperUnusedMethodCsvTool {
    private static final String ROOT_PATH = "D:\\git\\usCode\\dbu-mod-delivery";

    private static final String JAVA_SUFFIX = ".java";
    private static final int SCAN_PROGRESS_INTERVAL = 200;
    private static final Set<String> EXCLUDED_MAPPER_FULL_NAMES = new HashSet<>(Arrays.asList(
            "com.cds.airwaybillno.mapper.AirwaybillnoDetailMapper",
            "com.cds.airwaybillno.mapper.AirwaybillnoInfoMapper"
    ));

    private static final Map<String, ClassContext> classContextMap = new HashMap<>();
    private static final Map<MethodRef, MapperMethodInfo> mapperMethodInfoMap = new HashMap<>();
    private static final Set<String> mapperInterfaceSet = new LinkedHashSet<>();
    private static final Map<String, Set<String>> mapperSimpleToFullMap = new HashMap<>();
    private static final Set<MethodRef> referencedMapperMethods = new LinkedHashSet<>();

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
        final String mapperFullName;
        final String methodName;
        final int paramCount;

        MethodRef(String mapperFullName, String methodName, int paramCount) {
            this.mapperFullName = mapperFullName;
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
                    && mapperFullName.equals(other.mapperFullName);
        }

        @Override
        public int hashCode() {
            int result = mapperFullName.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + paramCount;
            return result;
        }
    }

    static class MapperMethodInfo {
        final MethodRef ref;
        final String sourceFile;
        final int lineNo;

        MapperMethodInfo(MethodRef ref, String sourceFile, int lineNo) {
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
        RuntimeConfig config = RuntimeConfig.of(ROOT_PATH, "unused_mapper_methods.csv");
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
        List<MapperMethodInfo> unusedMethods = collectUnusedMethods();
        writeCsv(config.projectRoot, config.outputCsv, unusedMethods);

        JavaParseLogUtils.logInfo(String.format(
                Locale.ROOT,
                "Done. javaRoots=%d, javaFiles=%d, mapperInterfaces=%d, mapperMethods=%d, referencedMethods=%d, unusedMethods=%d, elapsed=%dms",
                scannedJavaRootCount,
                scannedJavaFileCount,
                mapperInterfaceSet.size(),
                mapperMethodInfoMap.size(),
                referencedMapperMethods.size(),
                unusedMethods.size(),
                System.currentTimeMillis() - scanStartMillis
        ));
    }

    private static void resetState() {
        classContextMap.clear();
        mapperMethodInfoMap.clear();
        mapperInterfaceSet.clear();
        mapperSimpleToFullMap.clear();
        referencedMapperMethods.clear();
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
                    fieldTypeMap.put(var.getNameAsString(), JavaParseTextUtils.normalizeTypeName(var.getType().asString()))
            ));
            enrichServiceImplBaseMapperField(clazz, fieldTypeMap);

            boolean mapperInterface = isMapperInterface(clazz, pkg, projectRoot, javaFile);
            boolean effectiveMapperInterface = mapperInterface && !isExcludedMapper(fullName);
            if (effectiveMapperInterface) {
                registerMapperInterface(fullName);
                registerMapperMethods(clazz, fullName, projectRoot, javaFile);
            } else if (mapperInterface) {
                JavaParseLogUtils.logInfo("Skip excluded mapper: " + fullName);
            }

            clazz.getMethods().forEach(method -> collectMethodCalls(fullName, effectiveMapperInterface, method, fieldTypeMap, ctx));
        }
    }

    private static boolean isExcludedMapper(String mapperFullName) {
        return EXCLUDED_MAPPER_FULL_NAMES.contains(mapperFullName);
    }

    private static void enrichServiceImplBaseMapperField(
            ClassOrInterfaceDeclaration clazz,
            Map<String, String> fieldTypeMap
    ) {
        clazz.getExtendedTypes().forEach(ext -> {
            String parentType = JavaParseTextUtils.shortClassName(JavaParseTextUtils.normalizeTypeName(ext.getNameAsString()));
            if (!"ServiceImpl".equals(parentType)) {
                return;
            }
            ext.getTypeArguments().ifPresent(args -> {
                if (args.isEmpty()) {
                    return;
                }
                String mapperType = JavaParseTextUtils.normalizeTypeName(args.get(0).asString());
                if (mapperType == null || mapperType.isEmpty()) {
                    return;
                }
                fieldTypeMap.putIfAbsent("baseMapper", mapperType);
            });
        });
    }

    private static boolean isMapperInterface(
            ClassOrInterfaceDeclaration clazz,
            String pkg,
            Path projectRoot,
            Path javaFile
    ) {
        if (!clazz.isInterface()) {
            return false;
        }

        String className = clazz.getNameAsString();
        if (className.endsWith("Mapper")) {
            return true;
        }

        String pkgLower = pkg.toLowerCase(Locale.ROOT);
        if (pkgLower.equals("mapper")
                || pkgLower.endsWith(".mapper")
                || pkgLower.contains(".mapper.")) {
            return true;
        }

        String normalized = JavaMethodCallAnalysisUtils.relativizePath(projectRoot, javaFile).toLowerCase(Locale.ROOT);
        if (normalized.contains("/mapper/")) {
            return true;
        }

        return hasAnnotation(clazz, "Mapper") || hasAnnotation(clazz, "Repository");
    }

    private static boolean hasAnnotation(ClassOrInterfaceDeclaration clazz, String simpleName) {
        return clazz.getAnnotations().stream().anyMatch(ann -> {
            String name = ann.getNameAsString();
            return simpleName.equals(name) || simpleName.equals(JavaParseTextUtils.shortClassName(name));
        });
    }

    private static void registerMapperInterface(String mapperFullName) {
        mapperInterfaceSet.add(mapperFullName);
        mapperSimpleToFullMap.computeIfAbsent(JavaParseTextUtils.shortClassName(mapperFullName), key -> new LinkedHashSet<>()).add(mapperFullName);
    }

    private static void registerMapperMethods(
            ClassOrInterfaceDeclaration clazz,
            String mapperFullName,
            Path projectRoot,
            Path javaFile
    ) {
        clazz.getMethods().forEach(method -> {
            if (method.isStatic()) {
                return;
            }
            MethodRef ref = new MethodRef(mapperFullName, method.getNameAsString(), method.getParameters().size());

            String relativePath = JavaMethodCallAnalysisUtils.relativizePath(projectRoot, javaFile);
            int lineNo = method.getBegin().map(pos -> pos.line).orElse(-1);
            mapperMethodInfoMap.put(ref, new MapperMethodInfo(ref, relativePath, lineNo));
        });
    }

    private static void collectMethodCalls(
            String ownerClass,
            boolean ownerIsMapperInterface,
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
                if (ownerIsMapperInterface) {
                    referencedMapperMethods.add(new MethodRef(ownerClass, calledMethodName, argCount));
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
    }

    private static void resolveReferences() {
        for (ClassContext ctx : classContextMap.values()) {
            for (MethodCallRef call : ctx.methodCalls) {
                List<String> mapperCandidates = resolveMapperCandidates(call.targetTypeName, ctx);
                for (String mapperType : mapperCandidates) {
                    MethodRef ref = new MethodRef(mapperType, call.methodName, call.argCount);
                    if (mapperMethodInfoMap.containsKey(ref)) {
                        referencedMapperMethods.add(ref);
                    }
                }
            }
        }
    }

    private static List<String> resolveMapperCandidates(String rawTypeName, ClassContext ctx) {
        String typeName = JavaParseTextUtils.normalizeTypeName(rawTypeName);
        if (typeName == null || typeName.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (typeName.contains(".")) {
            if (mapperInterfaceSet.contains(typeName)) {
                result.add(typeName);
            }
            return new ArrayList<>(result);
        }

        String simpleName = JavaParseTextUtils.shortClassName(typeName);

        String imported = ctx.explicitImports.get(simpleName);
        if (imported != null && mapperInterfaceSet.contains(imported)) {
            result.add(imported);
        }

        if (ctx.pkg != null && !ctx.pkg.isEmpty()) {
            String samePackage = ctx.pkg + "." + simpleName;
            if (mapperInterfaceSet.contains(samePackage)) {
                result.add(samePackage);
            }
        }

        for (String pkg : ctx.onDemandImports) {
            String candidate = pkg + "." + simpleName;
            if (mapperInterfaceSet.contains(candidate)) {
                result.add(candidate);
            }
        }

        Set<String> fallback = mapperSimpleToFullMap.get(simpleName);
        if (fallback != null) {
            result.addAll(fallback);
        }

        return new ArrayList<>(result);
    }

    private static List<MapperMethodInfo> collectUnusedMethods() {
        List<MapperMethodInfo> unused = new ArrayList<>();
        for (Map.Entry<MethodRef, MapperMethodInfo> entry : mapperMethodInfoMap.entrySet()) {
            if (!referencedMapperMethods.contains(entry.getKey())) {
                unused.add(entry.getValue());
            }
        }
        unused.sort(Comparator
                .comparing((MapperMethodInfo item) -> item.ref.mapperFullName)
                .thenComparing(item -> item.ref.methodName)
                .thenComparingInt(item -> item.ref.paramCount)
                .thenComparing(item -> item.sourceFile)
                .thenComparingInt(item -> item.lineNo));
        return unused;
    }

    private static void writeCsv(String projectRoot, String outputCsv, List<MapperMethodInfo> rows) throws Exception {
        Path outputPath = Paths.get(outputCsv).toAbsolutePath().normalize();

        String lineSeparator = System.lineSeparator();
        StringBuilder content = new StringBuilder(256 + rows.size() * 128);
        content.append("project_root,mapper_interface,method_name,param_count,source_file,line_number")
                .append(lineSeparator);
        for (MapperMethodInfo row : rows) {
            content.append(CsvIOUtils.csvAlwaysQuote(projectRoot)).append(",")
                    .append(CsvIOUtils.csvAlwaysQuote(row.ref.mapperFullName)).append(",")
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
                "Scan progress: javaFiles=%d, mapperInterfaces=%d, mapperMethods=%d, references=%d",
                scannedJavaFileCount,
                mapperInterfaceSet.size(),
                mapperMethodInfoMap.size(),
                referencedMapperMethods.size()
        ));
    }

}
