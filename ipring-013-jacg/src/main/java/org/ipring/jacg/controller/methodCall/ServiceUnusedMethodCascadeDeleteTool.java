package org.ipring.jacg.controller.methodCall;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Realtime parse project, find unused service-interface methods, cascade delete java methods and mapper SQL.
 */
public class ServiceUnusedMethodCascadeDeleteTool {

    private static final String JAVA_SUFFIX = ".java";
    private static final String XML_SUFFIX = ".xml";
    private static final int SCAN_PROGRESS_INTERVAL = 200;
    private static final List<String> SQL_TAGS = Arrays.asList("select", "insert", "update", "delete");
    private static final Set<String> EXCLUDED_SERVICE_FULL_NAMES = new HashSet<>(Arrays.asList(
            "com.cds.base.service.AuthApiService"
    ));

    private static final Map<String, ClassInfo> classInfoMap = new HashMap<>();
    private static final Map<MethodKey, MethodMeta> methodMetaMap = new HashMap<>();
    private static final List<MethodCallRef> pendingCalls = new ArrayList<>();
    private static final Map<MethodKey, Set<MethodKey>> outgoingMap = new HashMap<>();
    private static final Map<MethodKey, Set<MethodKey>> incomingMap = new HashMap<>();
    private static final Map<String, Set<String>> simpleNameToFullClass = new HashMap<>();
    private static final Map<String, Set<String>> interfaceToImpl = new HashMap<>();

    private static int scannedJavaFileCount = 0;
    private static int scannedJavaRootCount = 0;
    private static int touchedJavaFiles = 0;
    private static int deletedJavaMethods = 0;
    private static int deletedJavaFields = 0;
    private static int deletedJavaFiles = 0;
    private static int touchedXmlFiles = 0;
    private static int deletedXmlStatements = 0;
    private static long startMillis = 0L;

    static class RuntimeConfig {
        final String projectRoot;

        RuntimeConfig(String projectRoot) {
            this.projectRoot = projectRoot;
        }

        static RuntimeConfig fromArgs(String[] args) {
            String root = args != null && args.length > 0 && !clean(args[0]).isEmpty()
                    ? clean(args[0])
                    : "D:\\git\\usCode\\dbu-mod-delivery";
            return new RuntimeConfig(root);
        }
    }

    static class MethodKey {
        final String ownerClass;
        final String methodName;
        final int paramCount;

        MethodKey(String ownerClass, String methodName, int paramCount) {
            this.ownerClass = ownerClass;
            this.methodName = methodName;
            this.paramCount = paramCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodKey)) {
                return false;
            }
            MethodKey other = (MethodKey) obj;
            return paramCount == other.paramCount
                    && ownerClass.equals(other.ownerClass)
                    && methodName.equals(other.methodName);
        }

        @Override
        public int hashCode() {
            int result = ownerClass.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + paramCount;
            return result;
        }
    }

    static class MethodMeta {
        final MethodKey key;
        final Path javaFile;
        final String sourceFile;

        MethodMeta(MethodKey key, Path javaFile, String sourceFile) {
            this.key = key;
            this.javaFile = javaFile;
            this.sourceFile = sourceFile;
        }
    }

    static class MethodCallRef {
        final MethodKey caller;
        final String callerClass;
        final String targetTypeName;
        final String methodName;
        final int argCount;
        final boolean localCall;

        MethodCallRef(
                MethodKey caller,
                String callerClass,
                String targetTypeName,
                String methodName,
                int argCount,
                boolean localCall
        ) {
            this.caller = caller;
            this.callerClass = callerClass;
            this.targetTypeName = targetTypeName;
            this.methodName = methodName;
            this.argCount = argCount;
            this.localCall = localCall;
        }
    }

    static class ClassInfo {
        final String fullName;
        final String pkg;
        final boolean isInterface;
        final Map<String, String> explicitImports;
        final List<String> onDemandImports;
        final Map<String, String> fieldTypeMap;
        final List<String> implementedTypes;

        ClassInfo(
                String fullName,
                String pkg,
                boolean isInterface,
                Map<String, String> explicitImports,
                List<String> onDemandImports,
                Map<String, String> fieldTypeMap,
                List<String> implementedTypes
        ) {
            this.fullName = fullName;
            this.pkg = pkg;
            this.isInterface = isInterface;
            this.explicitImports = explicitImports;
            this.onDemandImports = onDemandImports;
            this.fieldTypeMap = fieldTypeMap;
            this.implementedTypes = implementedTypes;
        }
    }

    static class LineRange {
        int startLine;
        int endLine;

        LineRange(int startLine, int endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }

    static class XmlDeleteResult {
        final String content;
        final int removed;

        XmlDeleteResult(String content, int removed) {
            this.content = content;
            this.removed = removed;
        }
    }

    public static void main(String[] args) throws Exception {
        RuntimeConfig config = RuntimeConfig.fromArgs(args);
        Path projectRoot = Paths.get(config.projectRoot).toAbsolutePath().normalize();
        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Project path invalid: " + projectRoot);
        }

        resetState();
        logInfo("Tool start. projectRoot=" + projectRoot);

        scanProject(projectRoot);
        buildInterfaceImplMap();
        resolveCallGraph();
        addInterfaceImplDispatchEdges();

        Set<MethodKey> initialUnused = findInitialUnusedServiceMethods();
        if (initialUnused.isEmpty()) {
            logInfo("No unused service methods found.");
            return;
        }
        logInfo("Initial unused service methods: " + initialUnused.size());

        Set<MethodKey> deleteSet = cascadeDeleteSet(initialUnused);
        logInfo("Cascade delete methods: " + deleteSet.size());

        deleteMethodsInJava(projectRoot, deleteSet);
        deleteMapperStatementsInXml(projectRoot, deleteSet);

        logInfo(String.format(
                Locale.ROOT,
                "Done. javaRoots=%d, javaFiles=%d, initialUnused=%d, deleteSet=%d, touchedJavaFiles=%d, deletedJavaMethods=%d, deletedJavaFields=%d, deletedJavaFiles=%d, touchedXmlFiles=%d, deletedXmlStatements=%d, elapsed=%dms",
                scannedJavaRootCount,
                scannedJavaFileCount,
                initialUnused.size(),
                deleteSet.size(),
                touchedJavaFiles,
                deletedJavaMethods,
                deletedJavaFields,
                deletedJavaFiles,
                touchedXmlFiles,
                deletedXmlStatements,
                System.currentTimeMillis() - startMillis
        ));
    }

    private static void resetState() {
        classInfoMap.clear();
        methodMetaMap.clear();
        pendingCalls.clear();
        outgoingMap.clear();
        incomingMap.clear();
        simpleNameToFullClass.clear();
        interfaceToImpl.clear();

        scannedJavaFileCount = 0;
        scannedJavaRootCount = 0;
        touchedJavaFiles = 0;
        deletedJavaMethods = 0;
        deletedJavaFields = 0;
        deletedJavaFiles = 0;
        touchedXmlFiles = 0;
        deletedXmlStatements = 0;
        startMillis = System.currentTimeMillis();
    }

    private static void scanProject(Path projectRoot) throws Exception {
        List<Path> javaRoots = findAllJavaRoots(projectRoot);
        if (javaRoots.isEmpty()) {
            logWarn("No src/main/java found under: " + projectRoot);
            return;
        }

        for (Path javaRoot : javaRoots) {
            scannedJavaRootCount++;
            logInfo("Scanning java root: " + javaRoot);
            scanJavaRoot(javaRoot, projectRoot);
        }
    }

    private static List<Path> findAllJavaRoots(Path root) throws Exception {
        List<Path> roots = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> "java".equalsIgnoreCase(path.getFileName().toString()))
                    .forEach(path -> {
                        Path parent = path.getParent();
                        if (parent == null || parent.getFileName() == null) {
                            return;
                        }
                        if (!"main".equalsIgnoreCase(parent.getFileName().toString())) {
                            return;
                        }
                        Path grand = parent.getParent();
                        if (grand == null || grand.getFileName() == null) {
                            return;
                        }
                        if (!"src".equalsIgnoreCase(grand.getFileName().toString())) {
                            return;
                        }
                        String normalized = path.toString().replace('\\', '/');
                        if (normalized.contains("/target/") || normalized.contains("/build/")) {
                            return;
                        }
                        roots.add(path.toAbsolutePath().normalize());
                    });
        }
        roots.sort(Comparator.comparing(Path::toString));
        return roots;
    }

    private static void scanJavaRoot(Path javaRoot, Path projectRoot) throws Exception {
        List<Path> javaFiles = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(javaRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(JAVA_SUFFIX))
                    .forEach(javaFiles::add);
        }
        javaFiles.sort(Comparator.comparing(Path::toString));

        for (Path javaFile : javaFiles) {
            scannedJavaFileCount++;
            logScanProgressIfNeeded();
            try {
                CompilationUnit cu = StaticJavaParser.parse(javaFile);
                processCompilationUnit(cu, javaFile, projectRoot);
            } catch (Exception ex) {
                logWarn("Skip parse error: " + javaFile + " | " + clean(ex.getMessage()));
            }
        }
    }

    private static void processCompilationUnit(CompilationUnit cu, Path javaFile, Path projectRoot) {
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        Map<String, String> explicitImports = new HashMap<>();
        List<String> onDemandImports = new ArrayList<>();
        for (ImportDeclaration imp : cu.getImports()) {
            if (imp.isStatic()) {
                continue;
            }
            String imported = clean(imp.getNameAsString());
            if (imported.isEmpty()) {
                continue;
            }
            if (imp.isAsterisk()) {
                onDemandImports.add(imported);
            } else {
                explicitImports.put(shortName(imported), imported);
            }
        }

        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = clazz.getNameAsString();
            String fullName = pkg.isEmpty() ? className : pkg + "." + className;
            simpleNameToFullClass.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(fullName);

            Map<String, String> fieldTypeMap = new HashMap<>();
            clazz.getFields().forEach(field -> field.getVariables().forEach(var ->
                    fieldTypeMap.put(var.getNameAsString(), normalizeTypeName(var.getType().asString()))
            ));
            enrichServiceImplBaseMapperField(clazz, fieldTypeMap);

            List<String> implementedTypes = clazz.getImplementedTypes().stream()
                    .map(type -> normalizeTypeName(type.getNameAsString()))
                    .filter(value -> value != null && !value.isEmpty())
                    .collect(Collectors.toList());

            classInfoMap.put(fullName, new ClassInfo(
                    fullName,
                    pkg,
                    clazz.isInterface(),
                    new HashMap<>(explicitImports),
                    new ArrayList<>(onDemandImports),
                    fieldTypeMap,
                    implementedTypes
            ));

            clazz.getMethods().forEach(method -> {
                MethodKey key = new MethodKey(fullName, method.getNameAsString(), method.getParameters().size());
                String sourceFile;
                try {
                    sourceFile = projectRoot.relativize(javaFile.toAbsolutePath().normalize()).toString().replace('\\', '/');
                } catch (Exception ex) {
                    sourceFile = javaFile.toString().replace('\\', '/');
                }
                methodMetaMap.put(key, new MethodMeta(key, javaFile, sourceFile));
                collectMethodCalls(fullName, method, fieldTypeMap, key);
            });
        }
    }

    private static void enrichServiceImplBaseMapperField(
            ClassOrInterfaceDeclaration clazz,
            Map<String, String> fieldTypeMap
    ) {
        clazz.getExtendedTypes().forEach(ext -> {
            String parentType = shortName(normalizeTypeName(ext.getNameAsString()));
            if (!"ServiceImpl".equals(parentType)) {
                return;
            }
            ext.getTypeArguments().ifPresent(args -> {
                if (args.isEmpty()) {
                    return;
                }
                String mapperType = normalizeTypeName(args.get(0).asString());
                if (mapperType == null || mapperType.isEmpty()) {
                    return;
                }
                fieldTypeMap.putIfAbsent("baseMapper", mapperType);
            });
        });
    }

    private static void collectMethodCalls(
            String ownerClass,
            MethodDeclaration method,
            Map<String, String> fieldTypeMap,
            MethodKey callerKey
    ) {
        if (!method.getBody().isPresent()) {
            return;
        }
        Map<String, String> localVarTypeMap = buildLocalVarTypeMap(method);
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String calledMethodName = call.getNameAsString();
            int argCount = call.getArguments().size();

            if (!call.getScope().isPresent()) {
                pendingCalls.add(new MethodCallRef(callerKey, ownerClass, null, calledMethodName, argCount, true));
                return;
            }

            String scopeExpr = clean(call.getScope().get().toString());
            String scopeVar = normalizeScopeVar(scopeExpr);
            if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
                pendingCalls.add(new MethodCallRef(callerKey, ownerClass, null, calledMethodName, argCount, true));
                return;
            }

            String typeName = inferScopeType(scopeExpr, localVarTypeMap, fieldTypeMap);
            if (typeName == null || typeName.isEmpty()) {
                return;
            }
            pendingCalls.add(new MethodCallRef(callerKey, ownerClass, typeName, calledMethodName, argCount, false));
        });

        method.findAll(MethodReferenceExpr.class).forEach(ref -> {
            String calledMethodName = ref.getIdentifier();
            String scopeExpr = clean(ref.getScope().toString());
            String scopeVar = normalizeScopeVar(scopeExpr);
            if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
                pendingCalls.add(new MethodCallRef(callerKey, ownerClass, null, calledMethodName, -1, true));
                return;
            }

            String typeName = inferScopeType(scopeExpr, localVarTypeMap, fieldTypeMap);
            if (typeName == null || typeName.isEmpty()) {
                return;
            }
            pendingCalls.add(new MethodCallRef(callerKey, ownerClass, typeName, calledMethodName, -1, false));
        });
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

    private static void buildInterfaceImplMap() {
        for (ClassInfo clazz : classInfoMap.values()) {
            if (clazz.isInterface) {
                continue;
            }
            for (String rawIface : clazz.implementedTypes) {
                List<String> ifaceCandidates = resolveClassCandidates(rawIface, clazz.fullName);
                for (String iface : ifaceCandidates) {
                    ClassInfo ifaceInfo = classInfoMap.get(iface);
                    if (ifaceInfo != null && ifaceInfo.isInterface) {
                        interfaceToImpl.computeIfAbsent(iface, k -> new LinkedHashSet<>()).add(clazz.fullName);
                    }
                }
            }
        }
    }

    private static void resolveCallGraph() {
        for (MethodCallRef call : pendingCalls) {
            if (call.localCall) {
                if (call.argCount >= 0) {
                    MethodKey callee = new MethodKey(call.callerClass, call.methodName, call.argCount);
                    addEdgeIfMethodExists(call.caller, callee);
                } else {
                    addEdgesByName(call.caller, call.callerClass, call.methodName);
                }
                continue;
            }

            List<String> candidates = resolveClassCandidates(call.targetTypeName, call.callerClass);
            for (String ownerClass : candidates) {
                if (call.argCount >= 0) {
                    MethodKey callee = new MethodKey(ownerClass, call.methodName, call.argCount);
                    addEdgeIfMethodExists(call.caller, callee);
                } else {
                    addEdgesByName(call.caller, ownerClass, call.methodName);
                }

                Set<String> impls = interfaceToImpl.get(ownerClass);
                if (impls != null) {
                    for (String impl : impls) {
                        if (call.argCount >= 0) {
                            MethodKey implCallee = new MethodKey(impl, call.methodName, call.argCount);
                            addEdgeIfMethodExists(call.caller, implCallee);
                        } else {
                            addEdgesByName(call.caller, impl, call.methodName);
                        }
                    }
                }
            }
        }
    }

    private static void addEdgesByName(MethodKey caller, String ownerClass, String methodName) {
        for (MethodKey candidate : methodMetaMap.keySet()) {
            if (!ownerClass.equals(candidate.ownerClass)) {
                continue;
            }
            if (!methodName.equals(candidate.methodName)) {
                continue;
            }
            addEdgeIfMethodExists(caller, candidate);
        }
    }

    private static void addInterfaceImplDispatchEdges() {
        for (Map.Entry<String, Set<String>> entry : interfaceToImpl.entrySet()) {
            String iface = entry.getKey();
            Set<String> impls = entry.getValue();
            for (MethodKey interfaceMethod : methodMetaMap.keySet()) {
                if (!iface.equals(interfaceMethod.ownerClass)) {
                    continue;
                }
                for (String impl : impls) {
                    MethodKey implMethod = new MethodKey(impl, interfaceMethod.methodName, interfaceMethod.paramCount);
                    addEdgeIfMethodExists(interfaceMethod, implMethod);
                }
            }
        }
    }

    private static void addEdgeIfMethodExists(MethodKey caller, MethodKey callee) {
        if (!methodMetaMap.containsKey(callee)) {
            return;
        }
        outgoingMap.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
        incomingMap.computeIfAbsent(callee, k -> new LinkedHashSet<>()).add(caller);
    }

    private static Set<MethodKey> findInitialUnusedServiceMethods() {
        Set<MethodKey> result = new LinkedHashSet<>();
        for (MethodKey method : methodMetaMap.keySet()) {
            if (!isTargetServiceInterface(method.ownerClass)) {
                continue;
            }
            Set<MethodKey> callers = incomingMap.getOrDefault(method, Collections.emptySet());
            if (callers.isEmpty()) {
                result.add(method);
            }
        }
        return result;
    }

    private static boolean isTargetServiceInterface(String fullClassName) {
        ClassInfo info = classInfoMap.get(fullClassName);
        if (info == null || !info.isInterface) {
            return false;
        }
        if (EXCLUDED_SERVICE_FULL_NAMES.contains(fullClassName)) {
            return false;
        }
        return shortName(fullClassName).toLowerCase(Locale.ROOT).contains("service");
    }

    private static Set<MethodKey> cascadeDeleteSet(Set<MethodKey> initial) {
        Set<MethodKey> deleteSet = new LinkedHashSet<>(initial);
        Deque<MethodKey> queue = new ArrayDeque<>(initial);

        while (!queue.isEmpty()) {
            MethodKey current = queue.pollFirst();
            Set<MethodKey> callees = outgoingMap.getOrDefault(current, Collections.emptySet());
            for (MethodKey callee : callees) {
                if (deleteSet.contains(callee)) {
                    continue;
                }

                Set<MethodKey> callers = incomingMap.getOrDefault(callee, Collections.emptySet());
                boolean usedOutsideDeleteSet = false;
                for (MethodKey caller : callers) {
                    if (!deleteSet.contains(caller) && !caller.equals(callee)) {
                        usedOutsideDeleteSet = true;
                        break;
                    }
                }
                if (!usedOutsideDeleteSet) {
                    deleteSet.add(callee);
                    queue.addLast(callee);
                }
            }
        }
        return deleteSet;
    }

    private static void deleteMethodsInJava(Path projectRoot, Set<MethodKey> deleteSet) throws Exception {
        Map<Path, Set<MethodKey>> fileDeleteMap = new HashMap<>();
        for (MethodKey key : deleteSet) {
            MethodMeta meta = methodMetaMap.get(key);
            if (meta == null) {
                continue;
            }
            fileDeleteMap.computeIfAbsent(meta.javaFile.toAbsolutePath().normalize(), k -> new LinkedHashSet<>()).add(key);
        }

        List<Path> files = new ArrayList<>(fileDeleteMap.keySet());
        files.sort(Comparator.comparing(Path::toString));

        for (Path javaFile : files) {
            if (!Files.exists(javaFile)) {
                continue;
            }
            Set<MethodKey> fileTargets = fileDeleteMap.get(javaFile);
            int deleted = deleteMethodsInSingleFile(javaFile, fileTargets);
            if (deleted > 0) {
                touchedJavaFiles++;
                deletedJavaMethods += deleted;
                int deletedPrivate = removeUnreferencedPrivateMethodsWhenClassOnlyPrivate(javaFile);
                deletedJavaMethods += deletedPrivate;
                int deletedFields = removeUnusedFieldsInFile(javaFile);
                deletedJavaFields += deletedFields;
                int deletedFieldsAfterPrivate = removeUnusedFieldsInFile(javaFile);
                deletedJavaFields += deletedFieldsAfterPrivate;
                if (shouldDeleteJavaFile(javaFile)) {
                    Files.deleteIfExists(javaFile);
                    deletedJavaFiles++;
                    logInfo("Java file deleted (class emptied): " + javaFile);
                }
            }
        }
    }

    private static int deleteMethodsInSingleFile(Path javaFile, Set<MethodKey> fileTargets) throws Exception {
        List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        List<LineRange> ranges = new ArrayList<>();
        int found = 0;
        for (MethodKey target : fileTargets) {
            MethodDeclaration method = findTargetMethod(cu, pkg, target);
            if (method == null) {
                continue;
            }
            LineRange range = calculateDeleteRange(method, lines);
            if (range != null) {
                ranges.add(range);
                found++;
            }
        }

        if (ranges.isEmpty()) {
            return 0;
        }

        List<LineRange> merged = mergeRanges(ranges);
        for (int i = merged.size() - 1; i >= 0; i--) {
            LineRange range = merged.get(i);
            int from = Math.max(0, range.startLine - 1);
            int to = Math.min(lines.size(), range.endLine);
            if (from < to) {
                lines.subList(from, to).clear();
            }
        }

        writeLinesWithFileChannel(javaFile, lines);
        logInfo("Java updated: " + javaFile + ", deletedMethods=" + found);
        return found;
    }

    private static int removeUnreferencedPrivateMethodsWhenClassOnlyPrivate(Path javaFile) throws Exception {
        if (!Files.exists(javaFile)) {
            return 0;
        }
        List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
        CompilationUnit cu = StaticJavaParser.parse(javaFile);

        List<LineRange> ranges = new ArrayList<>();
        int removedCount = 0;
        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            List<MethodDeclaration> methods = clazz.getMethods();
            if (methods.isEmpty()) {
                continue;
            }

            boolean allPrivate = methods.stream().allMatch(MethodDeclaration::isPrivate);
            if (!allPrivate) {
                continue;
            }

            Map<String, MethodDeclaration> signatureToMethod = new HashMap<>();
            Map<String, Set<String>> graph = new HashMap<>();
            for (MethodDeclaration method : methods) {
                String signature = localMethodSignature(method.getNameAsString(), method.getParameters().size());
                signatureToMethod.put(signature, method);
            }

            for (MethodDeclaration method : methods) {
                String from = localMethodSignature(method.getNameAsString(), method.getParameters().size());
                Set<String> targets = collectLocalMethodTargets(method, signatureToMethod);
                graph.put(from, targets);
            }

            Set<String> roots = new LinkedHashSet<>();
            for (ConstructorDeclaration constructor : clazz.getConstructors()) {
                roots.addAll(collectLocalMethodTargets(constructor, signatureToMethod));
            }
            for (InitializerDeclaration initializer : clazz.getMembers().stream()
                    .filter(member -> member instanceof InitializerDeclaration)
                    .map(member -> (InitializerDeclaration) member)
                    .collect(Collectors.toList())) {
                roots.addAll(collectLocalMethodTargets(initializer, signatureToMethod));
            }
            for (FieldDeclaration field : clazz.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    var.getInitializer().ifPresent(init ->
                            roots.addAll(collectLocalMethodTargets(init, signatureToMethod))
                    );
                }
            }

            Set<String> reachable = new HashSet<>();
            Deque<String> queue = new ArrayDeque<>(roots);
            while (!queue.isEmpty()) {
                String current = queue.pollFirst();
                if (!reachable.add(current)) {
                    continue;
                }
                for (String next : graph.getOrDefault(current, Collections.emptySet())) {
                    if (!reachable.contains(next)) {
                        queue.addLast(next);
                    }
                }
            }

            for (Map.Entry<String, MethodDeclaration> entry : signatureToMethod.entrySet()) {
                if (reachable.contains(entry.getKey())) {
                    continue;
                }
                LineRange range = calculateDeleteRange(entry.getValue(), lines);
                if (range != null) {
                    ranges.add(range);
                    removedCount++;
                }
            }
        }

        if (ranges.isEmpty()) {
            return 0;
        }
        List<LineRange> merged = mergeRanges(ranges);
        for (int i = merged.size() - 1; i >= 0; i--) {
            LineRange range = merged.get(i);
            int from = Math.max(0, range.startLine - 1);
            int to = Math.min(lines.size(), range.endLine);
            if (from < to) {
                lines.subList(from, to).clear();
            }
        }
        writeLinesWithFileChannel(javaFile, lines);
        logInfo("Java updated: " + javaFile + ", deletedPrivateMethods=" + removedCount);
        return removedCount;
    }

    private static int removeUnusedFieldsInFile(Path javaFile) throws Exception {
        if (!Files.exists(javaFile)) {
            return 0;
        }
        List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
        CompilationUnit cu = StaticJavaParser.parse(javaFile);

        List<LineRange> fieldRanges = new ArrayList<>();
        int removedCount = 0;
        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!isServiceLikeType(clazz.getNameAsString())) {
                continue;
            }
            Set<String> usedNames = collectUsedFieldNames(clazz);
            for (FieldDeclaration field : clazz.getFields()) {
                boolean allUnused = true;
                for (VariableDeclarator var : field.getVariables()) {
                    if (usedNames.contains(var.getNameAsString())) {
                        allUnused = false;
                        break;
                    }
                }
                if (!allUnused) {
                    continue;
                }
                LineRange range = calculateFieldDeleteRange(field, lines);
                if (range != null) {
                    fieldRanges.add(range);
                    removedCount++;
                }
            }
        }

        if (fieldRanges.isEmpty()) {
            return 0;
        }
        List<LineRange> merged = mergeRanges(fieldRanges);
        for (int i = merged.size() - 1; i >= 0; i--) {
            LineRange range = merged.get(i);
            int from = Math.max(0, range.startLine - 1);
            int to = Math.min(lines.size(), range.endLine);
            if (from < to) {
                lines.subList(from, to).clear();
            }
        }
        writeLinesWithFileChannel(javaFile, lines);
        logInfo("Java updated: " + javaFile + ", deletedFields=" + removedCount);
        return removedCount;
    }

    private static boolean isServiceLikeType(String typeName) {
        return clean(typeName).toLowerCase(Locale.ROOT).contains("service");
    }

    private static Set<String> collectUsedFieldNames(ClassOrInterfaceDeclaration clazz) {
        Set<String> used = new HashSet<>();
        for (NameExpr expr : clazz.findAll(NameExpr.class)) {
            used.add(expr.getNameAsString());
        }
        for (FieldAccessExpr expr : clazz.findAll(FieldAccessExpr.class)) {
            String scope = clean(expr.getScope().toString());
            if ("this".equals(scope) || "super".equals(scope)) {
                used.add(expr.getNameAsString());
            }
        }
        return used;
    }

    private static LineRange calculateFieldDeleteRange(FieldDeclaration field, List<String> lines) {
        if (!field.getBegin().isPresent() || !field.getEnd().isPresent()) {
            return null;
        }

        int startLine = field.getBegin().get().line;
        int endLine = field.getEnd().get().line;
        field.getAnnotations().forEach(ann -> {
            if (ann.getBegin().isPresent()) {
                // noop for lambda scope
            }
        });
        for (com.github.javaparser.ast.expr.AnnotationExpr ann : field.getAnnotations()) {
            if (ann.getBegin().isPresent()) {
                startLine = Math.min(startLine, ann.getBegin().get().line);
            }
        }

        int cursor = startLine - 1;
        while (cursor - 1 >= 0) {
            String prev = lines.get(cursor - 1).trim();
            if (prev.isEmpty()) {
                startLine = cursor;
                cursor--;
                continue;
            }
            if (prev.startsWith("//")) {
                startLine = cursor;
                cursor--;
                continue;
            }
            if (prev.endsWith("*/")) {
                int blockStart = findBlockCommentStart(lines, cursor - 1);
                startLine = blockStart + 1;
                cursor = blockStart;
                continue;
            }
            break;
        }
        return new LineRange(startLine, endLine);
    }

    private static MethodDeclaration findTargetMethod(CompilationUnit cu, String pkg, MethodKey key) {
        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String fullName = pkg.isEmpty() ? clazz.getNameAsString() : pkg + "." + clazz.getNameAsString();
            if (!key.ownerClass.equals(fullName)) {
                continue;
            }
            for (MethodDeclaration method : clazz.getMethods()) {
                if (key.methodName.equals(method.getNameAsString())
                        && key.paramCount == method.getParameters().size()) {
                    return method;
                }
            }
        }
        return null;
    }

    private static LineRange calculateDeleteRange(MethodDeclaration method, List<String> lines) {
        if (!method.getBegin().isPresent() || !method.getEnd().isPresent()) {
            return null;
        }
        int startLine = method.getBegin().get().line;
        int endLine = method.getEnd().get().line;

        for (com.github.javaparser.ast.expr.AnnotationExpr ann : method.getAnnotations()) {
            if (ann.getBegin().isPresent()) {
                startLine = Math.min(startLine, ann.getBegin().get().line);
            }
        }

        int cursor = startLine - 1;
        while (cursor - 1 >= 0) {
            String prev = lines.get(cursor - 1).trim();
            if (prev.isEmpty()) {
                startLine = cursor;
                cursor--;
                continue;
            }
            if (prev.startsWith("//")) {
                startLine = cursor;
                cursor--;
                continue;
            }
            if (prev.endsWith("*/")) {
                int blockStart = findBlockCommentStart(lines, cursor - 1);
                startLine = blockStart + 1;
                cursor = blockStart;
                continue;
            }
            break;
        }
        return new LineRange(startLine, endLine);
    }

    private static Set<String> collectLocalMethodTargets(Node node, Map<String, MethodDeclaration> signatureToMethod) {
        Set<String> result = new LinkedHashSet<>();
        for (MethodCallExpr call : node.findAll(MethodCallExpr.class)) {
            if (!isLocalMethodCall(call)) {
                continue;
            }
            String signature = localMethodSignature(call.getNameAsString(), call.getArguments().size());
            if (signatureToMethod.containsKey(signature)) {
                result.add(signature);
            }
        }

        for (MethodReferenceExpr ref : node.findAll(MethodReferenceExpr.class)) {
            if (!isLocalMethodReference(ref)) {
                continue;
            }
            String methodName = ref.getIdentifier();
            for (String signature : signatureToMethod.keySet()) {
                if (signature.startsWith(methodName + "#")) {
                    result.add(signature);
                }
            }
        }
        return result;
    }

    private static boolean isLocalMethodCall(MethodCallExpr call) {
        if (!call.getScope().isPresent()) {
            return true;
        }
        String scopeExpr = clean(call.getScope().get().toString());
        String scopeVar = normalizeScopeVar(scopeExpr);
        return "this".equals(scopeVar) || "super".equals(scopeVar);
    }

    private static boolean isLocalMethodReference(MethodReferenceExpr ref) {
        String scopeExpr = clean(ref.getScope().toString());
        String scopeVar = normalizeScopeVar(scopeExpr);
        return "this".equals(scopeVar) || "super".equals(scopeVar);
    }

    private static String localMethodSignature(String methodName, int paramCount) {
        return clean(methodName) + "#" + paramCount;
    }

    private static int findBlockCommentStart(List<String> lines, int endIndexInclusive) {
        for (int i = endIndexInclusive; i >= 0; i--) {
            if (lines.get(i).contains("/*")) {
                return i;
            }
        }
        return endIndexInclusive;
    }

    private static List<LineRange> mergeRanges(List<LineRange> ranges) {
        if (ranges.isEmpty()) {
            return Collections.emptyList();
        }
        ranges.sort(Comparator.comparingInt((LineRange r) -> r.startLine).thenComparingInt(r -> r.endLine));
        List<LineRange> merged = new ArrayList<>();
        LineRange current = new LineRange(ranges.get(0).startLine, ranges.get(0).endLine);
        merged.add(current);
        for (int i = 1; i < ranges.size(); i++) {
            LineRange next = ranges.get(i);
            if (next.startLine <= current.endLine + 1) {
                current.endLine = Math.max(current.endLine, next.endLine);
            } else {
                current = new LineRange(next.startLine, next.endLine);
                merged.add(current);
            }
        }
        return merged;
    }

    private static boolean shouldDeleteJavaFile(Path javaFile) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            List<ClassOrInterfaceDeclaration> topTypes = cu.getTypes().stream()
                    .filter(type -> type instanceof ClassOrInterfaceDeclaration)
                    .map(type -> (ClassOrInterfaceDeclaration) type)
                    .collect(Collectors.toList());
            if (topTypes.isEmpty()) {
                return true;
            }
            for (ClassOrInterfaceDeclaration type : topTypes) {
                if (!isTopTypeTrulyEmpty(type)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            logWarn("Skip empty-file check parse error: " + javaFile + " | " + clean(ex.getMessage()));
            return false;
        }
    }

    private static boolean isTopTypeTrulyEmpty(ClassOrInterfaceDeclaration type) {
        if (!type.getMembers().isEmpty()) {
            return false;
        }
        if (!type.isInterface()) {
            return true;
        }
        if (!type.getAnnotations().isEmpty()) {
            return false;
        }
        if (type.getJavadocComment().isPresent()) {
            return false;
        }
        if (!type.getTypeParameters().isEmpty()) {
            return false;
        }
        if (!type.getExtendedTypes().isEmpty()) {
            return false;
        }
        if (!type.getImplementedTypes().isEmpty()) {
            return false;
        }
        return true;
    }

    private static void deleteMapperStatementsInXml(Path projectRoot, Set<MethodKey> deleteSet) throws Exception {
        Map<String, Set<String>> mapperMethodIdMap = new HashMap<>();
        for (MethodKey key : deleteSet) {
            if (!isMapperLikeClass(key.ownerClass)) {
                continue;
            }
            mapperMethodIdMap.computeIfAbsent(key.ownerClass, k -> new LinkedHashSet<>()).add(key.methodName);
        }
        if (mapperMethodIdMap.isEmpty()) {
            return;
        }

        List<Path> xmlRoots = findAllResourceRoots(projectRoot);
        for (Path xmlRoot : xmlRoots) {
            try (java.util.stream.Stream<Path> stream = Files.walk(xmlRoot)) {
                List<Path> xmlFiles = new ArrayList<>();
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(XML_SUFFIX))
                        .forEach(xmlFiles::add);
                xmlFiles.sort(Comparator.comparing(Path::toString));

                for (Path xmlFile : xmlFiles) {
                    int removed = deleteMapperStatementsInSingleXml(xmlFile, mapperMethodIdMap);
                    if (removed > 0) {
                        touchedXmlFiles++;
                        deletedXmlStatements += removed;
                    }
                }
            }
        }
    }

    private static int deleteMapperStatementsInSingleXml(Path xmlFile, Map<String, Set<String>> mapperMethodIdMap) {
        try {
            String content = new String(Files.readAllBytes(xmlFile), StandardCharsets.UTF_8);
            String namespace = parseMapperNamespace(content);
            if (namespace.isEmpty()) {
                return 0;
            }

            Set<String> ids = mapperMethodIdMap.get(namespace);
            if (ids == null || ids.isEmpty()) {
                return 0;
            }

            String updated = content;
            int removed = 0;
            for (String id : ids) {
                for (String tag : SQL_TAGS) {
                    XmlDeleteResult deleteResult = removeXmlStatementById(updated, tag, id);
                    updated = deleteResult.content;
                    removed += deleteResult.removed;
                }
            }
            if (removed > 0) {
                writeTextWithFileChannel(xmlFile, updated);
                logInfo("XML updated: " + xmlFile + ", deletedStatements=" + removed);
            }
            return removed;
        } catch (Exception ex) {
            logWarn("Skip xml parse/write error: " + xmlFile + " | " + clean(ex.getMessage()));
            return 0;
        }
    }

    private static String parseMapperNamespace(String content) {
        String text = content == null ? "" : content;
        Pattern pattern = Pattern.compile("(?is)<mapper\\b[^>]*\\bnamespace\\s*=\\s*(\"([^\"]*)\"|'([^']*)')[^>]*>");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        String namespace = matcher.group(2);
        if (namespace == null || namespace.isEmpty()) {
            namespace = matcher.group(3);
        }
        return clean(namespace);
    }

    private static XmlDeleteResult removeXmlStatementById(String xml, String tag, String id) {
        String idPattern = "(?:\"" + Pattern.quote(id) + "\"|'"
                + Pattern.quote(id) + "')";
        String regex = "(?is)<" + tag + "\\b[^>]*\\bid\\s*=\\s*" + idPattern
                + "[^>]*>.*?</" + tag + "\\s*>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(xml);

        int removed = 0;
        StringBuffer sb = new StringBuffer(xml.length());
        while (matcher.find()) {
            removed++;
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);
        return new XmlDeleteResult(sb.toString(), removed);
    }

    private static List<Path> findAllResourceRoots(Path root) throws Exception {
        List<Path> roots = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> "resources".equalsIgnoreCase(path.getFileName().toString()))
                    .forEach(path -> {
                        Path parent = path.getParent();
                        if (parent == null || parent.getFileName() == null) {
                            return;
                        }
                        if (!"main".equalsIgnoreCase(parent.getFileName().toString())) {
                            return;
                        }
                        Path grand = parent.getParent();
                        if (grand == null || grand.getFileName() == null) {
                            return;
                        }
                        if (!"src".equalsIgnoreCase(grand.getFileName().toString())) {
                            return;
                        }
                        String normalized = path.toString().replace('\\', '/');
                        if (normalized.contains("/target/") || normalized.contains("/build/")) {
                            return;
                        }
                        roots.add(path.toAbsolutePath().normalize());
                    });
        }
        roots.sort(Comparator.comparing(Path::toString));
        return roots;
    }

    private static List<String> resolveClassCandidates(String typeName, String contextClassName) {
        String type = normalizeTypeName(typeName);
        if (type == null || type.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (type.contains(".")) {
            result.add(type);
            return new ArrayList<>(result);
        }

        ClassInfo ctx = classInfoMap.get(contextClassName);
        if (ctx != null) {
            String imported = ctx.explicitImports.get(type);
            if (imported != null && !imported.isEmpty()) {
                result.add(imported);
            }
            if (ctx.pkg != null && !ctx.pkg.isEmpty()) {
                result.add(ctx.pkg + "." + type);
            }
            for (String onDemand : ctx.onDemandImports) {
                result.add(onDemand + "." + type);
            }
        }

        Set<String> fallback = simpleNameToFullClass.get(type);
        if (fallback != null) {
            result.addAll(fallback);
        }
        return new ArrayList<>(result);
    }

    private static boolean isMapperLikeClass(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return false;
        }
        String simple = shortName(fullClassName);
        if (simple.endsWith("Mapper")) {
            return true;
        }
        String lower = fullClassName.toLowerCase(Locale.ROOT);
        return lower.contains(".mapper.");
    }

    private static void writeLinesWithFileChannel(Path filePath, List<String> lines) throws Exception {
        String lineSeparator = System.lineSeparator();
        String text = String.join(lineSeparator, lines);
        if (!text.isEmpty() && !text.endsWith(lineSeparator)) {
            text = text + lineSeparator;
        }
        writeBytesWithFileChannel(filePath, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeTextWithFileChannel(Path filePath, String content) throws Exception {
        writeBytesWithFileChannel(filePath, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBytesWithFileChannel(Path filePath, byte[] bytes) throws Exception {
        try (FileChannel channel = FileChannel.open(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private static void logScanProgressIfNeeded() {
        if (scannedJavaFileCount <= 0 || scannedJavaFileCount % SCAN_PROGRESS_INTERVAL != 0) {
            return;
        }
        logInfo(String.format(
                Locale.ROOT,
                "Scan progress: javaFiles=%d, classes=%d, methods=%d, pendingCalls=%d",
                scannedJavaFileCount,
                classInfoMap.size(),
                methodMetaMap.size(),
                pendingCalls.size()
        ));
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private static String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String type = clean(typeName);
        if (type.isEmpty()) {
            return null;
        }
        int genericPos = type.indexOf('<');
        if (genericPos >= 0) {
            type = type.substring(0, genericPos).trim();
        }
        while (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2).trim();
        }
        return type;
    }

    private static String shortName(String fullName) {
        if (fullName == null) {
            return "";
        }
        int idx = fullName.lastIndexOf('.');
        return idx < 0 ? fullName : fullName.substring(idx + 1);
    }

    private static String normalizeScopeVar(String scopeExpr) {
        String scope = clean(scopeExpr);
        if (scope.startsWith("this.")) {
            return scope.substring("this.".length());
        }
        if (scope.startsWith("super.")) {
            return scope.substring("super.".length());
        }
        return scope;
    }

    private static boolean isTypeLikeScope(String scopeExpr) {
        String value = clean(scopeExpr);
        if (value.isEmpty()) {
            return false;
        }
        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            return false;
        }
        return Character.isUpperCase(value.charAt(0)) || value.contains(".");
    }

    private static void logInfo(String message) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] [INFO] " + message);
    }

    private static void logWarn(String message) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] [WARN] " + message);
    }
}
