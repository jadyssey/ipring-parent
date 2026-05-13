package org.ipring.jacg.controller;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.ipring.jacg.util.JavaMethodCallAnalysisUtils;
import org.ipring.jacg.util.JavaParseLogUtils;
import org.ipring.jacg.util.JavaParseTextUtils;
import org.ipring.jacg.util.JavaSourceScanUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 方法独占引用查找与删除工具 V1
 * <p>
 * 功能：
 * 1. 扫描指定项目的 Java 源码，建立完整的方法调用图
 * 2. 以指定入口方法为根，向下展开调用树
 * 3. 分析调用树中的每个方法是否为"独占引用"（仅被入口方法子树中的方法调用）
 * 4. 输出独占引用方法列表供确认
 * 5. 在用户确认后，从源文件中安全删除这些方法
 */
public class ExclusiveMethodFinderV1 {

    // ========== 常量 ==========
    private static final String JAVA_SUFFIX = ".java";
    private static final int MAX_EXPAND_LEVEL = 120;
    private static final String DEFAULT_PROJECT_PATH = "D:\\git\\usCode\\dbu-mod-delivery";
    private static final String DEFAULT_CLASS_NAME = "MisWaybillInfoController";
    private static final String DEFAULT_METHOD_NAME = "getOrderNumber";

    // ========== 项目扫描缓存（与 V6 共用结构） ==========

    /** fullClassName.methodName -> List<MethodDeclaration>（重载方法列表） */
    private static final Map<String, List<MethodDeclaration>> methodMap = new HashMap<>();

    /** interfaceSimpleName -> List<implFullClassName> */
    private static final Map<String, List<String>> interfaceToImpl = new HashMap<>();

    /** fullClassName -> fieldName -> fieldType */
    private static final Map<String, Map<String, String>> classFieldMap = new HashMap<>();

    /** fullClassName -> packageName */
    private static final Map<String, String> classPackageMap = new HashMap<>();

    /** fullClassName -> simpleName -> fullQualifiedImport */
    private static final Map<String, Map<String, String>> classImportTypeMap = new HashMap<>();

    /** fullClassName -> List<onDemandPackage> */
    private static final Map<String, List<String>> classImportOnDemandPackageMap = new HashMap<>();

    /** simpleClassName -> Set<fullClassName> */
    private static final Map<String, Set<String>> simpleNameToFullClass = new HashMap<>();

    /** fullClassName -> 源文件路径 */
    private static final Map<String, File> classFileMap = new HashMap<>();

    // ========== 调用图 ==========

    /** callerKey -> Set<calleeKey>，callerKey = "fullClass.methodName" */
    private static final Map<String, Set<String>> callerToCalleeMap = new HashMap<>();

    /** calleeKey -> Set<callerKey>，反向索引 */
    private static final Map<String, Set<String>> calleeToCallerMap = new HashMap<>();

    // ========== 统计 ==========
    private static int scannedJavaFileCount = 0;
    private static int totalMethodCount = 0;
    private static int totalCallEdgeCount = 0;

    // ========================================================================
    //  入口
    // ========================================================================

    public static void main(String[] args) throws Exception {
        // 1. 解析命令行参数
        EntryConfig config = parseArgs(args);

        JavaParseLogUtils.logInfo("========================================");
        JavaParseLogUtils.logInfo("方法独占引用查找与删除工具 V1 启动");
        JavaParseLogUtils.logInfo("  项目路径: " + config.projectPath);
        JavaParseLogUtils.logInfo("  入口类:   " + config.className);
        JavaParseLogUtils.logInfo("  入口方法: " + config.methodName);
        JavaParseLogUtils.logInfo("========================================");

        // 2. 扫描项目
        scanProject(config.projectPath);

        // 3. 构建调用图
        buildCallGraph();

        JavaParseLogUtils.logInfo(String.format(Locale.ROOT,
                "扫描完成: Java文件=%d, 方法=%d, 调用边=%d",
                scannedJavaFileCount, totalMethodCount, totalCallEdgeCount));

        // 4. 解析入口方法，展开调用链
        String entryKey = resolveEntryMethodKey(config.className, config.methodName);
        if (entryKey == null) {
            System.err.println("[ERROR] 未找到入口方法: " + config.className + "." + config.methodName);
            return;
        }
        JavaParseLogUtils.logInfo("入口方法解析为: " + entryKey);

        Set<String> callTreeKeys = expandCallTree(entryKey);
        JavaParseLogUtils.logInfo("调用树展开完成，共 " + callTreeKeys.size() + " 个方法");

        // 5. 全面的调用者分析（增强版）
        List<MethodCallerAnalysis> analyses = analyzeAllMethods(entryKey, callTreeKeys);
        List<String> exclusiveMethods = extractExclusiveMethods(analyses);

        // 6. 输出详细分析报告
        printAnalysisReport(entryKey, analyses);

        // 7. 确认并删除
        if (exclusiveMethods.isEmpty()) {
            JavaParseLogUtils.logInfo("没有发现可删除的独占引用方法。");
            return;
        }
        promptAndDelete(exclusiveMethods, callTreeKeys);
    }

    // ========================================================================
    //  参数解析
    // ========================================================================

    /**
     * 命令行参数：<projectPath> <className> <methodName>
     * 示例：D:\\project\\my-app MyService myMethod
     */
    static EntryConfig parseArgs(String[] args) {
        String projectPath = args.length > 0 && !args[0].trim().isEmpty()
                ? args[0].trim() : DEFAULT_PROJECT_PATH;
        String className = args.length > 1 && !args[1].trim().isEmpty()
                ? args[1].trim() : DEFAULT_CLASS_NAME;
        String methodName = args.length > 2 && !args[2].trim().isEmpty()
                ? args[2].trim() : DEFAULT_METHOD_NAME;
        return new EntryConfig(projectPath, className, methodName);
    }

    // ========================================================================
    //  项目扫描
    // ========================================================================

    /**
     * 扫描项目根目录及其子模块中的所有 Java 源文件，建立方法索引和类信息缓存。
     */
    static void scanProject(String rootPath) throws Exception {
        JavaParseLogUtils.logInfo("开始扫描项目: " + rootPath);
        File root = new File(rootPath);
        if (!root.exists()) {
            JavaParseLogUtils.logWarn("项目路径不存在: " + rootPath);
            return;
        }

        File rootJava = new File(root, "src/main/java");
        if (rootJava.exists()) {
            scanDirectory(rootJava);
        }

        // 扫描子模块
        for (File module : JavaSourceScanUtils.listFiles(root)) {
            if (!module.isDirectory()) continue;
            File src = new File(module, "src/main/java");
            if (src.exists()) {
                scanDirectory(src);
            }
        }

        JavaParseLogUtils.logInfo("项目扫描完成");
    }

    /**
     * 递归扫描目录下的所有 Java 文件，建立索引。
     */
    static void scanDirectory(File dir) throws Exception {
        for (File file : JavaSourceScanUtils.listFiles(dir)) {
            if (file.isDirectory()) {
                scanDirectory(file);
                continue;
            }
            if (!file.getName().endsWith(JAVA_SUFFIX)) continue;

            scannedJavaFileCount++;
            parseJavaFile(file, dir);
        }
    }

    /**
     * 解析单个 Java 文件，提取类、方法、字段、导入等信息。
     */
    static void parseJavaFile(File file, File srcRoot) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(file);
        String pkg = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString()).orElse("");

        JavaMethodCallAnalysisUtils.ImportContext importContext =
                JavaMethodCallAnalysisUtils.buildImportContext(cu.getImports());

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            String className = clazz.getNameAsString();
            String fullName = pkg.isEmpty() ? className : pkg + "." + className;

            simpleNameToFullClass
                    .computeIfAbsent(className, k -> new LinkedHashSet<>())
                    .add(fullName);
            classPackageMap.put(fullName, pkg);
            classImportTypeMap.put(fullName, new HashMap<>(importContext.getExplicitImports()));
            classImportOnDemandPackageMap.put(fullName, new ArrayList<>(importContext.getOnDemandImports()));
            classFileMap.put(fullName, file);

            // 建立方法索引
            clazz.getMethods().forEach(method ->
                    methodMap.computeIfAbsent(fullName + "." + method.getNameAsString(), k -> new ArrayList<>())
                            .add(method)
            );

            // 建立字段映射
            Map<String, String> fieldMap = new HashMap<>();
            clazz.getFields().forEach(field -> {
                String type = field.getElementType().asString();
                field.getVariables().forEach(var ->
                        fieldMap.put(var.getNameAsString(), type));
            });

            // 从 ServiceImpl<Mapper> 推断 baseMapper 字段
            clazz.getExtendedTypes().forEach(ext -> {
                String parent = shortName(normalizeTypeName(ext.getNameAsString()));
                if (!"ServiceImpl".equals(parent)) return;
                ext.getTypeArguments().ifPresent(args -> {
                    if (args.isEmpty()) return;
                    String mapperType = normalizeTypeName(args.get(0).asString());
                    if (mapperType != null && !mapperType.isEmpty()) {
                        fieldMap.putIfAbsent("baseMapper", mapperType);
                    }
                });
            });
            classFieldMap.put(fullName, fieldMap);

            // 建立接口->实现类映射
            clazz.getImplementedTypes().forEach(i -> {
                String iface = normalizeTypeName(i.getNameAsString());
                if (iface == null || iface.isEmpty()) return;
                addInterfaceImpl(iface, fullName);
                addInterfaceImpl(shortName(iface), fullName);
            });
        });
    }

    // ========================================================================
    //  调用图构建
    // ========================================================================

    /**
     * 遍历所有已索引方法，解析方法体中的调用关系，构建调用图。
     */
    static void buildCallGraph() {
        JavaParseLogUtils.logInfo("开始构建调用图...");
        int processed = 0;

        for (Map.Entry<String, List<MethodDeclaration>> entry : methodMap.entrySet()) {
            String fullMethodKey = entry.getKey();
            // fullMethodKey = "fullClassName.methodName"
            int dotIdx = fullMethodKey.lastIndexOf('.');
            if (dotIdx <= 0) continue;
            String className = fullMethodKey.substring(0, dotIdx);
            String methodName = fullMethodKey.substring(dotIdx + 1);

            for (MethodDeclaration method : entry.getValue()) {
                processed++;
                if (processed % 5000 == 0) {
                    JavaParseLogUtils.logInfo("  调用图构建进度: " + processed + " / 方法总数");
                }

                if (!method.getBody().isPresent()) continue;

                Map<String, String> localVarTypeMap =
                        JavaMethodCallAnalysisUtils.buildLocalVarTypeMap(method);
                Map<String, String> fieldMap =
                        classFieldMap.getOrDefault(className, Collections.emptyMap());

                Set<String> calledMethods = new LinkedHashSet<>();
                collectCalledMethodsFromBody(
                        method.getBody().get(),
                        className, method, localVarTypeMap, fieldMap,
                        calledMethods
                );

                if (!calledMethods.isEmpty()) {
                    String callerKey = className + "." + methodName + methodSignature(method);
                    callerToCalleeMap.put(callerKey, calledMethods);
                    totalCallEdgeCount += calledMethods.size();

                    for (String calleeKey : calledMethods) {
                        calleeToCallerMap
                                .computeIfAbsent(calleeKey, k -> new LinkedHashSet<>())
                                .add(callerKey);
                    }
                }
            }
        }

        totalMethodCount = processed;
        JavaParseLogUtils.logInfo("调用图构建完成，共 " + totalCallEdgeCount + " 条调用边");
    }

    /**
     * 判断一个 MethodCallExpr 是否属于当前正在处理的方法（ownerMethod）。
     * 通过遍历 AST 父链：如果遇到 ownerMethod 之前先遇到其他 MethodDeclaration，
     * 说明该调用属于匿名内部类 / lambda / 局部类中的方法，应跳过。
     * 如果到达 ownerMethod 之前没有遇到其他 MethodDeclaration，则属于当前方法。
     */
    static boolean isCallOwnedByCurrentMethod(MethodCallExpr call, MethodDeclaration ownerMethod) {
        // 从 call 的父节点开始向上遍历 AST 父链
        Optional<Node> parentOpt = call.getParentNode();
        while (parentOpt.isPresent()) {
            Node parent = parentOpt.get();
            // 到达当前方法声明节点 → 调用属当前方法
            if (parent == ownerMethod) return true;
            // 到达其他方法声明节点（匿名内部类 / lambda / 局部类中的方法）→ 该调用不属于当前方法
            if (parent instanceof MethodDeclaration) return false;
            parentOpt = parent.getParentNode();
        }
        // 未找到任何方法声明（理论上不会发生）→ 归属当前方法（兜底）
        return true;
    }

    /**
     * 递归收集方法体中的所有被调用方法，返回其唯一标识键。
     */
    static void collectCalledMethodsFromBody(
            BlockStmt body,
            String className,
            MethodDeclaration ownerMethod,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap,
            Set<String> result
    ) {
        for (Statement stmt : body.getStatements()) {
            collectCalledMethodsFromStatement(stmt, className, ownerMethod,
                    localVarTypeMap, fieldMap, result);
        }
    }

    /**
     * 递归遍历语句，收集所有被调用的方法。
     */
    static void collectCalledMethodsFromStatement(
            Statement stmt,
            String className,
            MethodDeclaration ownerMethod,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap,
            Set<String> result
    ) {
        if (stmt.isBlockStmt()) {
            for (Statement s : stmt.asBlockStmt().getStatements()) {
                collectCalledMethodsFromStatement(s, className, ownerMethod,
                        localVarTypeMap, fieldMap, result);
            }
            return;
        }

        // 控制流语句：递归处理其子语句
        if (stmt.isIfStmt()) {
            collectCalledMethodsFromStatement(stmt.asIfStmt().getThenStmt(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            stmt.asIfStmt().getElseStmt().ifPresent(e ->
                    collectCalledMethodsFromStatement(e, className, ownerMethod,
                            localVarTypeMap, fieldMap, result));
            return;
        }
        if (stmt.isForStmt()) {
            collectCalledMethodsFromStatement(stmt.asForStmt().getBody(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            return;
        }
        if (stmt.isForEachStmt()) {
            collectCalledMethodsFromStatement(stmt.asForEachStmt().getBody(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            return;
        }
        if (stmt.isWhileStmt()) {
            collectCalledMethodsFromStatement(stmt.asWhileStmt().getBody(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            return;
        }
        if (stmt.isTryStmt()) {
            collectCalledMethodsFromStatement(stmt.asTryStmt().getTryBlock(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            stmt.asTryStmt().getCatchClauses().forEach(c ->
                    collectCalledMethodsFromStatement(c.getBody(), className,
                            ownerMethod, localVarTypeMap, fieldMap, result));
            stmt.asTryStmt().getFinallyBlock().ifPresent(f ->
                    collectCalledMethodsFromStatement(f, className, ownerMethod,
                            localVarTypeMap, fieldMap, result));
            return;
        }

        // Do-while 和 switch：展开 body
        if (stmt.isDoStmt()) {
            collectCalledMethodsFromStatement(stmt.asDoStmt().getBody(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            return;
        }
        if (stmt.isSwitchStmt()) {
            stmt.asSwitchStmt().getEntries().forEach(entry ->
                    entry.getStatements().forEach(s ->
                            collectCalledMethodsFromStatement(s, className, ownerMethod,
                                    localVarTypeMap, fieldMap, result)));
            return;
        }
        // synchronized 块
        if (stmt.isSynchronizedStmt()) {
            collectCalledMethodsFromStatement(stmt.asSynchronizedStmt().getBody(),
                    className, ownerMethod, localVarTypeMap, fieldMap, result);
            return;
        }

        // 普通表达式语句：提取方法调用
        // 使用 findAll 递归查找所有方法调用表达式，但需过滤掉属于匿名类/lambda 作用域的调用
        stmt.findAll(MethodCallExpr.class).forEach(call -> {
            // 🔴 关键过滤：跳过属于匿名内部类、lambda、局部类中定义的方法的调用
            // 例如外层方法中的 new Thread(() -> SecurityUtils.getAuthentication())，
            // getAuthentication() 属于 lambda 的作用域，不应归到外层方法
            if (!isCallOwnedByCurrentMethod(call, ownerMethod)) return;

            String methodName = call.getNameAsString();
            int argCount = call.getArguments().size();
            List<String> argTypeHints = inferCallArgumentTypeHints(
                    call, localVarTypeMap, fieldMap);

            LinkedHashSet<String> targets = new LinkedHashSet<>();

            if (!call.getScope().isPresent()) {
                // 无作用域：当前类方法
                String calleeKey = tryResolveLocalMethod(className, methodName,
                        argCount, argTypeHints);
                if (calleeKey != null) targets.add(calleeKey);
            } else {
                String scopeExpr =
                        JavaParseTextUtils.normalizeInlineWhitespace(
                                call.getScope().get().toString());
                String scopeVar = JavaParseTextUtils.normalizeScopeVar(scopeExpr);

                if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
                    String calleeKey = tryResolveLocalMethod(className, methodName,
                            argCount, argTypeHints);
                    if (calleeKey != null) targets.add(calleeKey);
                }

                // 字段类型调用
                String fieldType = normalizeTypeName(fieldMap.get(scopeVar));
                if (fieldType != null) {
                    collectTargetsByType(targets, fieldType, methodName,
                            argCount, argTypeHints, className, false);
                } else if (JavaParseTextUtils.isTypeLikeScope(scopeExpr)) {
                    collectTargetsByType(targets, scopeExpr, methodName,
                            argCount, argTypeHints, className, true);
                }
            }

            for (String targetKey : targets) {
                result.add(targetKey);
            }
        });
    }

    /**
     * 尝试在当前类中解析方法，返回完整的 callerKey。
     */
    static String tryResolveLocalMethod(String className, String methodName,
                                         Integer argCount, List<String> argTypeHints) {
        MethodDeclaration resolved = resolveMethod(className, methodName,
                argCount, argTypeHints);
        if (resolved != null) {
            return className + "." + methodName + methodSignature(resolved);
        }
        return null;
    }

    // ========================================================================
    //  方法解析（复用 V6 核心逻辑）
    // ========================================================================

    /**
     * 根据类型名、接口实现解析方法调用的目标类。
     */
    static void collectTargetsByType(
            LinkedHashSet<String> targets,
            String typeName,
            String methodName,
            Integer argCount,
            List<String> argTypeHints,
            String contextClassName,
            boolean staticTypeScope
    ) {
        String type = normalizeTypeName(typeName);
        if (type == null) return;

        List<String> classCandidates = resolveClassCandidates(type, contextClassName);

        // 静态调用风格：找到第一个匹配的独立类
        if (staticTypeScope) {
            for (String candidate : classCandidates) {
                MethodDeclaration resolved = resolveMethod(candidate, methodName,
                        argCount, argTypeHints);
                if (resolved != null) {
                    targets.add(candidate + "." + methodName + methodSignature(resolved));
                    return;
                }
            }
        }

        // 接口/实现类调用
        boolean matchedInterfaceByExactType = false;
        for (String candidateType : classCandidates) {
            int before = targets.size();
            List<String> impls = interfaceToImpl.get(candidateType);
            if (impls != null) {
                for (String impl : impls) {
                    MethodDeclaration resolved = resolveMethod(impl, methodName,
                            argCount, argTypeHints);
                    if (resolved != null) {
                        targets.add(impl + "." + methodName + methodSignature(resolved));
                    }
                }
            }
            if (targets.size() > before) {
                matchedInterfaceByExactType = true;
            }
        }
        if (!matchedInterfaceByExactType) {
            addAll(targets, resolveByInterfaceOrType(type, methodName, argCount, argTypeHints));
            addAll(targets, resolveByInterfaceOrType(shortName(type), methodName, argCount, argTypeHints));
        }

        // 直接匹配
        for (String candidate : classCandidates) {
            MethodDeclaration resolved = resolveMethod(candidate, methodName,
                    argCount, argTypeHints);
            if (resolved != null) {
                targets.add(candidate + "." + methodName + methodSignature(resolved));
            }
        }
    }

    /**
     * 通过接口名查找实现类中的方法引用。
     */
    static Set<String> resolveByInterfaceOrType(
            String type, String methodName, Integer argCount,
            List<String> argTypeHints
    ) {
        Set<String> result = new LinkedHashSet<>();
        List<String> impls = interfaceToImpl.get(type);
        if (impls != null) {
            for (String impl : impls) {
                MethodDeclaration resolved = resolveMethod(impl, methodName,
                        argCount, argTypeHints);
                if (resolved != null) {
                    result.add(impl + "." + methodName + methodSignature(resolved));
                }
            }
        }
        return result;
    }

    // ========================================================================
    //  方法查找（直接引用 V6 核心方法）
    // ========================================================================

    static MethodDeclaration resolveMethod(String className, String methodName,
                                            Integer argCount, List<String> argTypeHints) {
        List<MethodDeclaration> candidates = methodMap.get(className + "." + methodName);
        if (candidates == null || candidates.isEmpty()) return null;

        if (argCount == null) {
            for (MethodDeclaration c : candidates) {
                if (c.getParameters().isEmpty()) return c;
            }
            return candidates.get(0);
        }

        List<MethodDeclaration> exact = new ArrayList<>();
        MethodDeclaration bestVarArgs = null;
        int bestVarArgsCount = -1;
        for (MethodDeclaration c : candidates) {
            int pc = c.getParameters().size();
            boolean va = !c.getParameters().isEmpty()
                    && c.getParameter(c.getParameters().size() - 1).isVarArgs();
            if (!va && pc == argCount) {
                exact.add(c);
            } else if (va && argCount >= pc - 1 && pc > bestVarArgsCount) {
                bestVarArgs = c;
                bestVarArgsCount = pc;
            }
        }

        if (!exact.isEmpty()) {
            if (exact.size() == 1) return exact.get(0);
            MethodDeclaration best = exact.get(0);
            int bestScore = Integer.MIN_VALUE;
            for (MethodDeclaration c : exact) {
                int score = scoreOverloadCandidate(c, argTypeHints);
                if (score > bestScore) {
                    best = c;
                    bestScore = score;
                }
            }
            return best;
        }
        return bestVarArgs;
    }

    // ========================================================================
    //  调用链展开
    // ========================================================================

    /**
     * 从入口方法键开始 BFS 展开调用树，返回所有可达方法键的集合（包含入口本身）。
     */
    static Set<String> expandCallTree(String entryKey) {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(entryKey);
        visited.add(entryKey);

        while (!queue.isEmpty()) {
            String current = queue.pollFirst();

            // 从当前方法中查找它调用了哪些方法
            Set<String> callees = callerToCalleeMap.get(current);
            if (callees == null) continue;

            for (String callee : callees) {
                if (visited.contains(callee)) continue;
                // 检查深度
                if (visited.size() > MAX_EXPAND_LEVEL) {
                    JavaParseLogUtils.logWarn("展开深度达到上限 " + MAX_EXPAND_LEVEL + "，截断");
                    return visited;
                }
                visited.add(callee);
                queue.addLast(callee);
            }
        }

        return visited;
    }

    // ========================================================================
    //  独占性分析（增强版）
    // ========================================================================

    /**
     * 存储单个方法的详细调用者分析结果
     */
    static class MethodCallerAnalysis {
        final String methodKey;
        /** 所有调用者（来自完整调用图） */
        final Set<String> allCallers;
        /** 位于调用树内的调用者 */
        final Set<String> treeCallers;
        /** 位于调用树外的调用者 */
        final Set<String> externalCallers;
        /** 是否为独占引用 */
        final boolean exclusive;

        MethodCallerAnalysis(String methodKey, Set<String> allCallers,
                             Set<String> treeCallers, Set<String> externalCallers,
                             boolean exclusive) {
            this.methodKey = methodKey;
            this.allCallers = allCallers;
            this.treeCallers = treeCallers;
            this.externalCallers = externalCallers;
            this.exclusive = exclusive;
        }
    }

    /**
     * 对调用树中每个方法进行完整的调用者分析。
     * 返回所有方法的详细分析结果（包括独占的和非独占的）。
     */
    static List<MethodCallerAnalysis> analyzeAllMethods(
            String entryKey, Set<String> callTreeKeys) {
        List<MethodCallerAnalysis> results = new ArrayList<>();

        for (String methodKey : callTreeKeys) {
            // 入口方法本身不参与分析
            if (methodKey.equals(entryKey)) continue;

            Set<String> allCallers = calleeToCallerMap.get(methodKey);
            if (allCallers == null || allCallers.isEmpty()) {
                // 无调用者的孤立方法：不属于任何调用链，跳过
                continue;
            }

            Set<String> treeCallers = new LinkedHashSet<>();
            Set<String> externalCallers = new LinkedHashSet<>();

            for (String caller : allCallers) {
                if (callTreeKeys.contains(caller)) {
                    treeCallers.add(caller);
                } else {
                    externalCallers.add(caller);
                }
            }

            boolean exclusive = externalCallers.isEmpty() && !treeCallers.isEmpty();
            results.add(new MethodCallerAnalysis(
                    methodKey, allCallers, treeCallers, externalCallers, exclusive));
        }

        // 排序：先独占，后非独占
        results.sort((a, b) -> {
            if (a.exclusive != b.exclusive) {
                return a.exclusive ? -1 : 1;
            }
            return a.methodKey.compareTo(b.methodKey);
        });

        return results;
    }

    /**
     * 从分析结果中提取独占方法列表。
     */
    static List<String> extractExclusiveMethods(List<MethodCallerAnalysis> analyses) {
        List<String> exclusive = new ArrayList<>();
        for (MethodCallerAnalysis analysis : analyses) {
            if (analysis.exclusive) {
                exclusive.add(analysis.methodKey);
            }
        }
        return exclusive;
    }

    /**
     * 从分析结果中提取非独占方法列表。
     */
    static List<String> extractNonExclusiveMethods(List<MethodCallerAnalysis> analyses) {
        List<String> nonExclusive = new ArrayList<>();
        for (MethodCallerAnalysis analysis : analyses) {
            if (!analysis.exclusive) {
                nonExclusive.add(analysis.methodKey);
            }
        }
        return nonExclusive;
    }

    // ========================================================================
    //  输出分析结果（增强版）
    // ========================================================================

    static void printAnalysisResult(String entryKey,
                                     List<String> exclusiveMethods,
                                     List<String> nonExclusiveMethods) {
        // ===== 重新构建完整分析明细 =====
        // 由于原有的 exclusive/nonExclusive 方法是旧的简单列表，
        // 我们重新用 `analyzeAllMethods` 获取完整明细后输出

        // 先从 entryKey 获取 callTreeKeys（从 main 方法中传递的话需要重新获取）
        // 这里我们用传入的列表重建一份完整分析
        // 但更可靠的方式是让 analyzeAllMethods 的结果在这里替代使用

        // 由于 main 方法已改为使用 analyzeAllMethods，此方法接收增强后的参数
        // 这里保持兼容性，让两个版本的打印都可用
        printAnalysisReport(entryKey, null);
    }

    /**
     * 新版详细报告输出方法。
     * analyses 为 null 时跳过（回退到旧版）。
     */
    static void printAnalysisReport(String entryKey,
                                     List<MethodCallerAnalysis> analyses) {
        System.out.println();
        System.out.println("================================================================");
        System.out.println("  方法独占引用分析报告（增强版）");
        System.out.println("================================================================");
        System.out.println("  入口方法: " + entryKey);
        System.out.println();

        if (analyses == null || analyses.isEmpty()) {
            System.out.println("  (调用树中无可分析的方法)");
            System.out.println();
            System.out.println("================================================================");
            System.out.println();
            return;
        }

        // ===== 统计概览 =====
        int exclusiveCount = 0;
        int nonExclusiveCount = 0;
        for (MethodCallerAnalysis a : analyses) {
            if (a.exclusive) exclusiveCount++;
            else nonExclusiveCount++;
        }

        System.out.println("  ┌─ 统计概览 ─────────────────────────────────────────────┐");
        System.out.println(String.format("  │  调用树中方法总数:     %4d                            │",
                analyses.size()));
        System.out.println(String.format("  │  独占方法（可安全删除）: %4d                            │",
                exclusiveCount));
        System.out.println(String.format("  │  非独占方法（不可删除）: %4d                            │",
                nonExclusiveCount));
        System.out.println("  └──────────────────────────────────────────────────────────┘");
        System.out.println();

        // ===== 详细列表 =====
        int idx = 0;
        for (MethodCallerAnalysis analysis : analyses) {
            idx++;
            String status = analysis.exclusive ? "✅ 独占" : "❌ 非独占";

            System.out.println(String.format(
                    "  [%4d] %s | %s", idx, status, analysis.methodKey));
            System.out.println(String.format(
                    "        调用者: %d 个 (树内: %d 个, 树外: %d 个)",
                    analysis.allCallers.size(),
                    analysis.treeCallers.size(),
                    analysis.externalCallers.size()));

            // 列出所有树内调用者
            if (!analysis.treeCallers.isEmpty()) {
                System.out.println("        ┌─ 树内调用者:");
                for (String treeCaller : analysis.treeCallers) {
                    System.out.println("        │  · " + treeCaller);
                }
            }

            // 列出所有树外调用者（关键信息！）
            if (!analysis.externalCallers.isEmpty()) {
                System.out.println("        └─ ⚠ 树外调用者（阻止独占判定的原因）：");
                for (String extCaller : analysis.externalCallers) {
                    System.out.println("           · " + extCaller);
                }
            } else {
                System.out.println("        └─ (无树外调用者)");
            }

            System.out.println();
        }

        System.out.println("================================================================");
        System.out.println();
    }

    // ========================================================================
    //  旧版独占性分析（保留用于向前兼容，但 main 中已改用增强版）
    // ========================================================================

    static List<String> findExclusiveMethods(String entryKey, Set<String> callTreeKeys) {
        List<String> exclusive = new ArrayList<>();
        for (String methodKey : callTreeKeys) {
            if (methodKey.equals(entryKey)) continue;
            Set<String> callers = calleeToCallerMap.get(methodKey);
            if (callers == null || callers.isEmpty()) continue;
            boolean allCallersInTree = true;
            for (String caller : callers) {
                if (!callTreeKeys.contains(caller)) {
                    allCallersInTree = false;
                    break;
                }
            }
            if (allCallersInTree) exclusive.add(methodKey);
        }
        return exclusive;
    }

    static List<String> findNonExclusiveMethods(String entryKey, Set<String> callTreeKeys) {
        List<String> nonExclusive = new ArrayList<>();
        for (String methodKey : callTreeKeys) {
            if (methodKey.equals(entryKey)) continue;
            Set<String> callers = calleeToCallerMap.get(methodKey);
            if (callers == null || callers.isEmpty()) continue;
            for (String caller : callers) {
                if (!callTreeKeys.contains(caller)) {
                    nonExclusive.add(methodKey);
                    break;
                }
            }
        }
        return nonExclusive;
    }

    // ========================================================================
    //  确认与删除
    // ========================================================================

    /**
     * 提示用户确认，并在确认后执行删除操作。
     */
    static void promptAndDelete(List<String> exclusiveMethods,
                                 Set<String> callTreeKeys) throws IOException {
        System.out.println("⚠  即将从源码中删除以下 " + exclusiveMethods.size() + " 个方法：");
        for (String key : exclusiveMethods) {
            System.out.println("    - " + key);
        }
        System.out.println();
        System.out.print("是否继续删除操作？(y/N): ");

        String input;
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
            input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        }

        if (!"y".equals(input) && !"yes".equals(input)) {
            JavaParseLogUtils.logInfo("用户取消删除操作。");
            return;
        }

        // 按文件分组，每个文件只解析一次
        Map<File, List<MethodDeleteInfo>> fileToMethods = groupByFile(exclusiveMethods);
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        JavaParseLogUtils.logInfo("开始执行删除操作...");

        for (Map.Entry<File, List<MethodDeleteInfo>> fileEntry : fileToMethods.entrySet()) {
            File sourceFile = fileEntry.getKey();
            List<MethodDeleteInfo> methodsToDelete = fileEntry.getValue();

            JavaParseLogUtils.logInfo("  处理文件: " + sourceFile.getAbsolutePath()
                    + " (" + methodsToDelete.size() + " 个方法)");

            try {
                boolean result = deleteMethodsFromFile(sourceFile, methodsToDelete);
                if (result) {
                    successCount += methodsToDelete.size();
                } else {
                    failCount += methodsToDelete.size();
                }
            } catch (Exception e) {
                System.err.println("[ERROR]  删除文件 " + sourceFile.getName()
                        + " 中的方法时出错: " + e.getMessage());
                failCount += methodsToDelete.size();
            }
        }

        System.out.println();
        JavaParseLogUtils.logInfo("删除操作完成。");
        JavaParseLogUtils.logInfo("  成功: " + successCount + " 个");
        JavaParseLogUtils.logInfo("  失败: " + failCount + " 个");
        JavaParseLogUtils.logInfo("  跳过: " + skipCount + " 个");
    }

    /**
     * 将待删除方法列表按源文件分组。
     */
    static Map<File, List<MethodDeleteInfo>> groupByFile(List<String> methodKeys) {
        Map<File, List<MethodDeleteInfo>> result = new LinkedHashMap<>();

        for (String key : methodKeys) {
            // key = "fullClassName.methodName(paramTypes)"
            int dotIdx = key.lastIndexOf('.');
            if (dotIdx <= 0) continue;
            String className = key.substring(0, dotIdx);

            File sourceFile = classFileMap.get(className);
            if (sourceFile == null || !sourceFile.exists()) {
                JavaParseLogUtils.logWarn("  无法找到源文件: " + className + " (key=" + key + ")");
                continue;
            }

            // 从 key 中提取方法名和参数签名
            // key 格式如 "com.example.Foo.bar(int, String)"
            String methodPart = key.substring(dotIdx + 1);
            int parenIdx = methodPart.indexOf('(');
            if (parenIdx <= 0) continue;
            String methodName = methodPart.substring(0, parenIdx);
            String paramsPart = methodPart.substring(parenIdx + 1, methodPart.length() - 1);

            result.computeIfAbsent(sourceFile, k -> new ArrayList<>())
                    .add(new MethodDeleteInfo(className, methodName, paramsPart, key));
        }

        return result;
    }

    /**
     * 从源文件中删除指定的方法列表。
     * 使用词法保留模式（LexicalPreservingPrinter）以保留文件格式。
     */
    static boolean deleteMethodsFromFile(File sourceFile,
                                          List<MethodDeleteInfo> methodsToDelete) throws IOException {
        if (methodsToDelete == null || methodsToDelete.isEmpty()) return true;

        // 读取原始内容
        String originalContent = new String(Files.readAllBytes(sourceFile.toPath()),
                StandardCharsets.UTF_8);

        // 使用词法保留模式解析
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(sourceFile);
        } catch (Exception e) {
            System.err.println("[ERROR]  无法解析文件: " + sourceFile.getAbsolutePath()
                    + " - " + e.getMessage());
            return false;
        }

        // 查找并删除每个方法
        int deletedCount = 0;
        for (MethodDeleteInfo info : methodsToDelete) {
            // 从 fullClassName 中提取简单类名
            String simpleClassName = shortName(info.fullClassName);

            // 在当前文件中查找匹配的类和方法
            boolean found = false;
            for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (!clazz.getNameAsString().equals(simpleClassName)) continue;

                for (MethodDeclaration method : clazz.getMethods()) {
                    if (!method.getNameAsString().equals(info.methodName)) continue;

                    // 验证参数签名（可选）
                    if (info.paramsPart != null && !info.paramsPart.isEmpty()) {
                        String actualParams = buildParamTypeSignature(method);
                        if (!actualParams.equals(info.paramsPart)
                                && !isCompatibleSignature(actualParams, info.paramsPart)) {
                            continue;
                        }
                    }

                    // 安全校验：跳过抽象方法（接口/抽象类）
                    if (method.isAbstract()) {
                        JavaParseLogUtils.logWarn("    跳过抽象方法: " + info.methodKey);
                        continue;
                    }

                    // 执行删除
                    try {
                        method.remove();
                        found = true;
                        deletedCount++;
                        JavaParseLogUtils.logInfo("    已删除: " + info.methodKey);
                    } catch (Exception e) {
                        System.err.println("[ERROR]    删除失败: " + info.methodKey
                                + " - " + e.getMessage());
                    }
                    break;
                }
                if (found) break;
            }

            if (!found) {
                JavaParseLogUtils.logWarn("    未在文件中找到方法: " + info.methodKey);
            }
        }

        if (deletedCount == 0) return true;

        // 将修改后的 AST 写回文件
        String newContent = cu.toString();
        // 对比前后内容
        if (newContent.equals(originalContent)) {
            JavaParseLogUtils.logWarn("    文件内容未发生变化，跳过写入: "
                    + sourceFile.getAbsolutePath());
            return false;
        }

        // 创建备份
        Path backupPath = sourceFile.toPath().resolveSibling(
                sourceFile.getName() + ".bak");
        Files.copy(sourceFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

        // 验证新内容的合法性（检查带注释的解析是否成功）
        try {
            StaticJavaParser.parse(newContent);
        } catch (Exception e) {
            System.err.println("[ERROR]    新内容解析失败，回滚操作: " + e.getMessage());
            return false;
        }

        // 写入新文件
        Files.write(sourceFile.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
        JavaParseLogUtils.logInfo("  文件已更新: " + sourceFile.getAbsolutePath()
                + " (备份: " + backupPath.getFileName() + ")");

        return true;
    }

    /**
     * 构建方法参数的类型签名（逗号分隔的类型名）。
     */
    static String buildParamTypeSignature(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(normalizeTypeName(method.getParameter(i).getType().asString()));
            if (method.getParameter(i).isVarArgs()) sb.append("...");
        }
        return sb.toString();
    }

    /**
     * 判断两个参数签名是否兼容（简化处理）。
     */
    static boolean isCompatibleSignature(String actual, String expected) {
        // 去除空格比较
        return normalizeSignature(actual).equals(normalizeSignature(expected));
    }

    static String normalizeSignature(String sig) {
        if (sig == null) return "";
        return sig.replaceAll("\\s+", "");
    }

    // ========================================================================
    //  入口方法解析
    // ========================================================================

    /**
     * 根据简单类名和方法名，解析出完整的入口方法键（带参数签名）。
     */
    static String resolveEntryMethodKey(String className, String methodName) {
        List<String> candidates = resolveEntryClassCandidates(className, methodName);
        if (candidates.isEmpty()) return null;

        String fullClass = candidates.get(0);
        MethodDeclaration method = resolveMethod(fullClass, methodName, null, null);
        if (method == null) return null;

        return fullClass + "." + methodName + methodSignature(method);
    }

    /**
     * 查找匹配的入口类。
     */
    static List<String> resolveEntryClassCandidates(String className, String methodName) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String type = normalizeTypeName(className);
        if (type == null || type.isEmpty()) return new ArrayList<>();

        if (type.contains(".")) {
            if (resolveMethod(type, methodName, null, null) != null) {
                result.add(type);
            }
        } else {
            Set<String> classes = simpleNameToFullClass.get(type);
            if (classes != null) {
                for (String full : classes) {
                    if (resolveMethod(full, methodName, null, null) != null) {
                        result.add(full);
                    }
                }
            }
        }
        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    // ========================================================================
    //  类型解析
    // ========================================================================

    static List<String> resolveClassCandidates(String typeName) {
        return resolveClassCandidates(typeName, null);
    }

    static List<String> resolveClassCandidates(String typeName, String contextClassName) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String type = normalizeTypeName(typeName);
        if (type == null || type.isEmpty()) return new ArrayList<>();

        String simple = shortName(type);

        if (type.contains(".")) {
            result.add(type);
            return new ArrayList<>(result);
        }

        if (contextClassName != null) {
            Map<String, String> explicitImports = classImportTypeMap.get(contextClassName);
            if (explicitImports != null) {
                String imported = explicitImports.get(simple);
                if (imported != null && !imported.isEmpty()) {
                    result.add(imported);
                    return new ArrayList<>(result);
                }
            }

            LinkedHashSet<String> contextCandidates = new LinkedHashSet<>();
            String pkg = classPackageMap.get(contextClassName);
            if (pkg != null && !pkg.isEmpty()) {
                contextCandidates.add(pkg + "." + simple);
            }

            List<String> onDemandImports = classImportOnDemandPackageMap.get(contextClassName);
            if (onDemandImports != null) {
                for (String importPkg : onDemandImports) {
                    if (importPkg == null || importPkg.isEmpty()) continue;
                    contextCandidates.add(importPkg + "." + simple);
                }
            }

            LinkedHashSet<String> known = new LinkedHashSet<>();
            for (String candidate : contextCandidates) {
                if (isScannedClass(candidate)) known.add(candidate);
            }
            if (!known.isEmpty()) return new ArrayList<>(known);
            if (!contextCandidates.isEmpty()) return new ArrayList<>(contextCandidates);
        }

        addAll(result, simpleNameToFullClass.get(type));
        addAll(result, simpleNameToFullClass.get(simple));
        return new ArrayList<>(result);
    }

    static boolean isScannedClass(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) return false;
        Set<String> classes = simpleNameToFullClass.get(shortName(fullClassName));
        return classes != null && classes.contains(fullClassName);
    }

    // ========================================================================
    //  工具方法
    // ========================================================================

    /**
     * 返回带方法名的完整签名，如 "methodName(int, String)"
     */
    static String methodDisplay(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder(method.getNameAsString()).append("(");
        appendParamTypes(method, sb);
        return sb.append(")").toString();
    }

    /**
     * 返回仅参数部分的签名，如 "(int, String)"，用于构造方法唯一键（不重复方法名）
     */
    static String methodSignature(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder("(");
        appendParamTypes(method, sb);
        return sb.append(")").toString();
    }

    private static void appendParamTypes(MethodDeclaration method, StringBuilder sb) {
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) sb.append(", ");
            String type = clean(method.getParameter(i).getType().asString());
            if (method.getParameter(i).isVarArgs()) {
                sb.append(type).append("...");
            } else {
                sb.append(type);
            }
        }
    }

    static List<String> inferCallArgumentTypeHints(
            MethodCallExpr call, Map<String, String> localVarTypeMap,
            Map<String, String> fieldTypeMap
    ) {
        List<String> hints = new ArrayList<>();
        for (Expression arg : call.getArguments()) {
            hints.add(inferExpressionTypeHint(arg, localVarTypeMap, fieldTypeMap));
        }
        return hints;
    }

    static String inferExpressionTypeHint(
            Expression expr, Map<String, String> localVarTypeMap,
            Map<String, String> fieldTypeMap
    ) {
        if (expr == null) return null;
        if (expr.isNullLiteralExpr()) return "null";
        if (expr.isStringLiteralExpr()) return "String";
        if (expr.isBooleanLiteralExpr()) return "boolean";
        if (expr.isCharLiteralExpr()) return "char";
        if (expr.isIntegerLiteralExpr()) return "int";
        if (expr.isLongLiteralExpr()) return "long";
        if (expr.isDoubleLiteralExpr()) return "double";
        if (expr.isObjectCreationExpr())
            return normalizeTypeName(expr.asObjectCreationExpr().getType().asString());
        if (expr.isCastExpr())
            return normalizeTypeName(expr.asCastExpr().getType().asString());
        if (expr.isNameExpr())
            return normalizeTypeName(localVarTypeMap.get(expr.asNameExpr().getNameAsString()));
        if (expr.isFieldAccessExpr()) {
            String scopeVar = JavaParseTextUtils.normalizeScopeVar(expr.toString());
            return normalizeTypeName(fieldTypeMap.get(scopeVar));
        }
        return null;
    }

    static int scoreOverloadCandidate(MethodDeclaration candidate, List<String> argTypeHints) {
        if (argTypeHints == null || argTypeHints.isEmpty()) return 0;
        int score = 0;
        int count = Math.min(candidate.getParameters().size(), argTypeHints.size());
        for (int i = 0; i < count; i++) {
            String paramType = normalizeTypeName(candidate.getParameter(i).getType().asString());
            String argType = normalizeTypeName(argTypeHints.get(i));
            score += scoreTypeCompatibility(paramType, argType);
        }
        return score;
    }

    static int scoreTypeCompatibility(String paramType, String argType) {
        if (argType == null || argType.isEmpty()) return 0;
        if ("null".equals(argType)) return isPrimitiveType(paramType) ? -50 : 1;
        String paramSimple = shortName(paramType);
        String argSimple = shortName(argType);
        if (paramType != null && argType != null
                && (paramType.equals(argType) || paramSimple.equals(argSimple))) return 8;
        if (isPrimitiveWrapperPair(paramSimple, argSimple)) return 6;
        if ("Object".equals(paramSimple)) return 1;
        return -4;
    }

    static boolean isPrimitiveType(String typeName) {
        if (typeName == null) return false;
        String t = shortName(typeName);
        return "byte".equals(t) || "short".equals(t) || "int".equals(t)
                || "long".equals(t) || "float".equals(t) || "double".equals(t)
                || "boolean".equals(t) || "char".equals(t);
    }

    static boolean isPrimitiveWrapperPair(String left, String right) {
        return ("byte".equals(left) && "Byte".equals(right))
                || ("Byte".equals(left) && "byte".equals(right))
                || ("short".equals(left) && "Short".equals(right))
                || ("Short".equals(left) && "short".equals(right))
                || ("int".equals(left) && "Integer".equals(right))
                || ("Integer".equals(left) && "int".equals(right))
                || ("long".equals(left) && "Long".equals(right))
                || ("Long".equals(left) && "long".equals(right))
                || ("float".equals(left) && "Float".equals(right))
                || ("Float".equals(left) && "float".equals(right))
                || ("double".equals(left) && "Double".equals(right))
                || ("Double".equals(left) && "double".equals(right))
                || ("boolean".equals(left) && "Boolean".equals(right))
                || ("Boolean".equals(left) && "boolean".equals(right))
                || ("char".equals(left) && "Character".equals(right))
                || ("Character".equals(left) && "char".equals(right));
    }

    static void addInterfaceImpl(String iface, String impl) {
        if (iface == null || iface.isEmpty()) return;
        List<String> impls = interfaceToImpl.computeIfAbsent(iface, k -> new ArrayList<>());
        if (!impls.contains(impl)) impls.add(impl);
    }

    static void addAll(LinkedHashSet<String> target, Collection<String> source) {
        if (source == null) return;
        target.addAll(source);
    }

    static String shortName(String s) {
        return JavaParseTextUtils.shortClassName(s);
    }

    static String normalizeTypeName(String typeName) {
        return JavaParseTextUtils.normalizeTypeName(typeName);
    }

    static String clean(String s) {
        return JavaParseTextUtils.normalizeInlineWhitespace(s);
    }

    // ========================================================================
    //  数据结构
    // ========================================================================

    /** 描述单个待分析工程的配置 */
    static class EntryConfig {
        final String projectPath;
        final String className;
        final String methodName;

        EntryConfig(String projectPath, String className, String methodName) {
            this.projectPath = projectPath;
            this.className = className;
            this.methodName = methodName;
        }
    }

    /** 记录方法删除元信息 */
    static class MethodDeleteInfo {
        final String fullClassName;
        final String methodName;
        final String paramsPart;
        final String methodKey;

        MethodDeleteInfo(String fullClassName, String methodName,
                         String paramsPart, String methodKey) {
            this.fullClassName = fullClassName;
            this.methodName = methodName;
            this.paramsPart = paramsPart;
            this.methodKey = methodKey;
        }
    }
}
