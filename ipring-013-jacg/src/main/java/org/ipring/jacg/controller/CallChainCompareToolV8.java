package org.ipring.jacg.controller;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.ipring.jacg.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * 当前主用版本的调用链对比工具，负责扫描源码、展开调用链并输出差异结果。
 * 1. 注释代码不比对
 * 2. Mapper文件格式化
 * 3. 生成时创建文件夹
 */
public class CallChainCompareToolV8 {

    private static final String JAVA_SUFFIX = ".java";
    private static final String XML_SUFFIX = ".xml";
    private static final List<String> SQL_TAGS = Arrays.asList("select", "insert", "update", "delete");
    private static final int MAX_EXPAND_LEVEL = 120;
    private static final int DIFF_CONTEXT_LINES = 2;
    private static final int SCAN_PROGRESS_INTERVAL = 200;
    private static final String DEFAULT_TARGET_FOLDER = "派送对比运单运单";
    private static final String DEFAULT_TARGET_FUNC = "下单运单";
    /**
     * 返回默认需要比对的工程及其入口方法。
     */
    private static List<ProjectConfig> defaultProjectConfigs() {
        return Arrays.asList(
                // new ProjectConfig(
                //         "omswaybill-admin",
                //         "D:\\project\\gfs-mod-omswaybill-admin",
                //         "MisWaybillInfoController",
                //         "getInfoByWaybillNo"
                // ),
                new ProjectConfig(
                        "waybill",
                        "/home/liu/IdeaProjects/dbu-mod-waybill",
                        "MisWaybillInfoServiceImpl",
                        "syncFormOrder"
                ),
                new ProjectConfig(
                        "delivery",
                        "/home/liu/IdeaProjects/dbu-mod-delivery",
                        "MisWaybillInfoServiceImpl",
                        "syncFormOrder"
                )
                // MisWaybillInfoServiceImpl#syncFormOrder
                // LocationConsumer#consumeMessage
                // OmsOrderController#add
        );
    }


    /**
     * 描述单个待比对工程的入口配置。
     */
    static class ProjectConfig {
        final String name;
        final String path;
        final String className;
        final String methodName;

        /**
         * 保存一个工程的名称、路径和调用链入口。
         */
        ProjectConfig(String name, String path, String className, String methodName) {
            this.name = name;
            this.path = path;
            this.className = className;
            this.methodName = methodName;
        }
    }

    /**
     * 保存运行时传入的输出目录和功能名称。
     */
    static class RuntimeConfig {
        final String targetFolder;
        final String targetFunc;

        /**
         * 记录本次输出目录和目标功能名称。
         */
        RuntimeConfig(String targetFolder, String targetFunc) {
            this.targetFolder = targetFolder;
            this.targetFunc = targetFunc;
        }

        /**
         * 使用命令行参数覆盖默认输出目录和目标功能名称。
         */
        static RuntimeConfig fromArgs(String[] args) {
            String folder = args.length > 0 && !JavaParseTextUtils.normalizeInlineWhitespace(args[0]).isEmpty()
                    ? args[0]
                    : DEFAULT_TARGET_FOLDER;
            String function = args.length > 1 && !JavaParseTextUtils.normalizeInlineWhitespace(args[1]).isEmpty()
                    ? args[1]
                    : DEFAULT_TARGET_FUNC;
            return new RuntimeConfig(folder, function);
        }
    }

    private static final Map<String, List<MethodDeclaration>> methodMap = new HashMap<>();
    private static final Map<String, List<String>> interfaceToImpl = new HashMap<>();
    private static final Map<String, Map<String, String>> classFieldMap = new HashMap<>();
    private static final Map<String, String> classPackageMap = new HashMap<>();
    private static final Map<String, Map<String, String>> classImportTypeMap = new HashMap<>();
    private static final Map<String, List<String>> classImportOnDemandPackageMap = new HashMap<>();
    private static final Map<String, Set<String>> simpleNameToFullClass = new HashMap<>();
    private static final Map<String, String> mapperSqlMap = new HashMap<>();
    /** 记录已完整展开过的方法（key = className.methodName(paramType1,paramType2,...)），避免重复输出内部实现。 */
    private static final Set<String> expandedMethods = new HashSet<>();
    private static int scannedJavaFileCount = 0;
    private static int scannedXmlFileCount = 0;
    private static long scanStartMillis = 0L;

    /**
     * 扫描配置工程、展开调用链并生成对比产物。
     */
    public static void main(String[] args) throws Exception {
        RuntimeConfig runtime = RuntimeConfig.fromArgs(args);
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        List<ProjectConfig> projectConfigs = defaultProjectConfigs();
        JavaParseLogUtils.logInfo("Tool start. targetFolder=" + runtime.targetFolder + ", targetFunc=" + runtime.targetFunc
                + ", projects=" + projectConfigs.size());

        File outputDir = new File(new File(runtime.targetFolder, runtime.targetFunc), date);
        ensureDirectory(outputDir);
        JavaParseLogUtils.logInfo("Output directory ready: " + outputDir.getAbsolutePath());

        List<String> outputFiles = new ArrayList<>();
        for (int i = 0; i < projectConfigs.size(); i++) {
            ProjectConfig config = projectConfigs.get(i);
            JavaParseLogUtils.logInfo(String.format(Locale.ROOT, "Project [%d/%d] start: %s", i + 1, projectConfigs.size(), config.name));
            resetCaches();
            scanAllModules(config.path);
            JavaParseLogUtils.logInfo(String.format(
                    Locale.ROOT,
                    "Project [%d/%d] scan done: java=%d, xml=%d, methods=%d, mapperSql=%d, elapsed=%dms",
                    i + 1,
                    projectConfigs.size(),
                    scannedJavaFileCount,
                    scannedXmlFileCount,
                    indexedMethodCount(),
                    mapperSqlMap.size(),
                    System.currentTimeMillis() - scanStartMillis
            ));

            String outputFile = new File(outputDir, config.name + "_" + date + ".log").getPath();
            outputFiles.add(outputFile);
            JavaParseLogUtils.logInfo("Generating callchain log: " + outputFile);
            writeCallChainLog(config, outputFile);
            JavaParseLogUtils.logInfo("Callchain log done: " + outputFile);
        }

        if (outputFiles.size() >= 2) {
            String html = new File(outputDir, "diff_" + date + ".html").getPath();
            JavaParseLogUtils.logInfo("Generating full diff html: " + html);
            generateHtml(outputFiles.get(0), outputFiles.get(1), html);
            String diffOnlyLog = new File(outputDir, "diff_only_" + date + ".log").getPath();
            JavaParseLogUtils.logInfo("Generating diff-only log: " + diffOnlyLog);
            generateDiffOnlyLog(outputFiles.get(0), outputFiles.get(1), diffOnlyLog, DIFF_CONTEXT_LINES);
            String diffOnlyHtml = new File(outputDir, "diff_only_" + date + ".html").getPath();
            JavaParseLogUtils.logInfo("Generating diff-only html: " + diffOnlyHtml);
            generateDiffOnlyHtml(outputFiles.get(0), outputFiles.get(1), diffOnlyHtml, DIFF_CONTEXT_LINES);
            JavaParseLogUtils.logInfo("Opening browser: " + diffOnlyHtml);
            openInBrowser(diffOnlyHtml);
            JavaParseLogUtils.logInfo("Diff generation done.");
        } else {
            JavaParseLogUtils.logInfo("Skip diff generation because output file count < 2.");
        }
        JavaParseLogUtils.logInfo("Tool finished.");
    }

    /**
     * 为单个工程输出调用链日志。
     */
    private static void writeCallChainLog(ProjectConfig config, String outputFile) throws Exception {
        JavaParseLogUtils.logInfo("Expand callchain entry: " + config.className + "." + config.methodName + " (" + config.name + ")");
        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8.name())) {
            writer.println("==== " + config.name + " ====");
            writer.println();
            expandBySimpleName(config.className, config.methodName, writer);
        }
    }

    /**
     * 尝试在桌面环境中直接打开生成的 HTML 文件。
     */
    private static void openInBrowser(String htmlPath) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new File(htmlPath).toURI());
        } catch (Exception ignore) {
            // Browser opening is optional.
        }
    }

    /**
     * 在处理下一个工程前清空所有扫描缓存。
     */
    private static void resetCaches() {
        methodMap.clear();
        interfaceToImpl.clear();
        classFieldMap.clear();
        classPackageMap.clear();
        classImportTypeMap.clear();
        classImportOnDemandPackageMap.clear();
        simpleNameToFullClass.clear();
        mapperSqlMap.clear();
        expandedMethods.clear();
        scannedJavaFileCount = 0;
        scannedXmlFileCount = 0;
        scanStartMillis = System.currentTimeMillis();
    }

    /**
     * 确保输出目录存在，避免后续写文件失败。
     */
    private static void ensureDirectory(File dir) {
        if (dir.exists()) {
            return;
        }
        if (!dir.mkdirs()) {
            throw new IllegalStateException("Failed to create output directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * 按固定间隔输出扫描进度，便于观察大工程扫描状态。
     */
    private static void logScanProgressIfNeeded() {
        int total = scannedJavaFileCount + scannedXmlFileCount;
        if (total <= 0 || total % SCAN_PROGRESS_INTERVAL != 0) {
            return;
        }
        JavaParseLogUtils.logInfo(String.format(
                Locale.ROOT,
                "Scan progress: java=%d, xml=%d, methods=%d, mapperSql=%d",
                scannedJavaFileCount,
                scannedXmlFileCount,
                indexedMethodCount(),
                mapperSqlMap.size()
        ));
    }

    /**
     * 扫描根工程及其子模块中的 Java 和 MyBatis XML 资源。
     */
    static void scanAllModules(String rootPath) throws Exception {
        File root = new File(rootPath);
        if (!root.exists()) {
            JavaParseLogUtils.logWarn("Project path not found: " + rootPath);
            return;
        }
        JavaParseLogUtils.logInfo("Scan start: " + root.getAbsolutePath());

        File rootJava = new File(root, "src/main/java");
        if (rootJava.exists()) {
            JavaParseLogUtils.logInfo("Scanning java root: " + rootJava.getAbsolutePath());
            scan(rootJava);
        }

        File rootResources = new File(root, "src/main/resources");
        if (rootResources.exists()) {
            JavaParseLogUtils.logInfo("Scanning xml root: " + rootResources.getAbsolutePath());
            scanMapperXml(rootResources);
        }

        for (File module : JavaSourceScanUtils.listFiles(root)) {
            if (!module.isDirectory()) {
                continue;
            }
            File src = new File(module, "src/main/java");
            if (src.exists()) {
                JavaParseLogUtils.logInfo("Scanning module java: " + src.getAbsolutePath());
                scan(src);
            }
            File resources = new File(module, "src/main/resources");
            if (resources.exists()) {
                JavaParseLogUtils.logInfo("Scanning module xml: " + resources.getAbsolutePath());
                scanMapperXml(resources);
            }
        }
    }

    /**
     * 递归扫描 Java 文件并建立方法、字段、导入和接口实现索引。
     */
    static void scan(File dir) throws Exception {
        for (File file : JavaSourceScanUtils.listFiles(dir)) {
            if (file.isDirectory()) {
                scan(file);
                continue;
            }

            if (!file.getName().endsWith(JAVA_SUFFIX)) {
                continue;
            }
            scannedJavaFileCount++;
            logScanProgressIfNeeded();

            CompilationUnit cu = StaticJavaParser.parse(file);
            String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
            JavaMethodCallAnalysisUtils.ImportContext importContext = JavaMethodCallAnalysisUtils.buildImportContext(cu.getImports());

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                String className = clazz.getNameAsString();
                String fullName = pkg.isEmpty() ? className : pkg + "." + className;
                simpleNameToFullClass.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(fullName);
                classPackageMap.put(fullName, pkg);
                classImportTypeMap.put(fullName, new HashMap<>(importContext.getExplicitImports()));
                classImportOnDemandPackageMap.put(fullName, new ArrayList<>(importContext.getOnDemandImports()));

                clazz.getMethods().forEach(method ->
                        methodMap.computeIfAbsent(fullName + "." + method.getNameAsString(), k -> new ArrayList<>())
                                .add(method)
                );

                Map<String, String> fieldMap = new HashMap<>();
                clazz.getFields().forEach(field -> {
                    String type = field.getElementType().asString();
                    field.getVariables().forEach(var -> fieldMap.put(var.getNameAsString(), type));
                });

                clazz.getExtendedTypes().forEach(ext -> {
                    String parent = shortName(normalizeTypeName(ext.getNameAsString()));
                    if (!"ServiceImpl".equals(parent)) {
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
                        fieldMap.putIfAbsent("baseMapper", mapperType);
                    });
                });

                classFieldMap.put(fullName, fieldMap);

                clazz.getImplementedTypes().forEach(i -> {
                    String iface = normalizeTypeName(i.getNameAsString());
                    if (iface == null || iface.isEmpty()) {
                        return;
                    }
                    addInterfaceImpl(iface, fullName);
                    addInterfaceImpl(shortName(iface), fullName);
                });
            });
        }
    }

    /**
     * 递归扫描 MyBatis Mapper XML 文件并提取 SQL 文本。
     */
    static void scanMapperXml(File dir) {
        for (File file : JavaSourceScanUtils.listFiles(dir)) {
            if (file.isDirectory()) {
                scanMapperXml(file);
                continue;
            }
            if (!file.getName().endsWith(XML_SUFFIX)) {
                continue;
            }
            scannedXmlFileCount++;
            logScanProgressIfNeeded();
            parseMapperXml(file);
        }
    }

    /**
     * 解析单个 Mapper XML 并缓存 `namespace.id -> SQL` 映射。
     */
    static void parseMapperXml(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", false);
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void fatalError(SAXParseException e) {
                    // Ignore non-mybatis xml or malformed xml.
                }
            });

            Document doc = builder.parse(file);
            Element root = doc.getDocumentElement();
            if (root == null || !"mapper".equals(root.getTagName())) {
                return;
            }

            String namespace = clean(root.getAttribute("namespace"));
            if (namespace.isEmpty()) {
                return;
            }

            for (String tag : SQL_TAGS) {
                NodeList nodes = root.getElementsByTagName(tag);
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node n = nodes.item(i);
                    if (n.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element stmt = (Element) n;
                    String id = clean(stmt.getAttribute("id"));
                    if (id.isEmpty()) {
                        continue;
                    }

                    String sql = ProjectMethodCompareSqlUtils.normalizeMapperSql(
                            ProjectMethodCompareXmlUtils.flattenXmlChildren(stmt)
                    );
                    if (sql.isEmpty()) {
                        continue;
                    }

                    mapperSqlMap.put(namespace + "." + id, tag.toUpperCase(Locale.ROOT) + " " + sql);
                }
            }
        } catch (Exception ignore) {
            // Ignore non-mybatis xml or malformed xml.
        }
    }

    /**
     * 通过简单类名定位入口方法，兼容同名类的多候选情况。
     */
    static void expandBySimpleName(String className, String methodName, PrintWriter writer) {
        List<String> classCandidates = resolveEntryClassCandidates(className, methodName);
        if (classCandidates.isEmpty()) {
            writer.println("Not found entry point: " + className + "." + methodName);
            return;
        }
        if (classCandidates.size() > 1) {
            writer.println("Ambiguous entry point (same simple class name): " + className + "." + methodName);
        }

        for (int i = 0; i < classCandidates.size(); i++) {
            String fullClass = classCandidates.get(i);
            if (classCandidates.size() > 1) {
                writer.println("Resolved entry: " + fullClass + "." + methodName);
            }
            expand(fullClass, methodName, null, null, 0, writer);
            if (i < classCandidates.size() - 1) {
                writer.println();
            }
        }
    }

    /**
     * 递归展开指定方法，输出方法签名、语句和下游调用。
     */
    static void expand(String className, String methodName, Integer argCount, List<String> argTypeHints, int level, PrintWriter writer) {
        if (level > MAX_EXPAND_LEVEL) {
            write(writer, level, "[max-depth] " + shortName(className) + "." + methodName);
            return;
        }

        MethodDeclaration method = resolveMethod(className, methodName, argCount, argTypeHints);
        if (method == null) {
            return;
        }

        String dedupKey = buildDedupKey(className, method);
        if (expandedMethods.contains(dedupKey)) {
            write(writer, level, shortName(className) + "." + methodDisplay(method) + " （已展开）");
            return;
        }

        expandedMethods.add(dedupKey);
        write(writer, level, shortName(className) + "." + methodDisplay(method));

        if (!method.getBody().isPresent()) {
            String mapperKey = className + "." + methodName;
            String sql = mapperSqlMap.get(mapperKey);
            if (sql != null && !sql.isEmpty()) {
                writeSqlBlock(writer, level + 1, sql);
            }
            return;
        }

        method.getBody().ifPresent(body -> {
            Map<String, String> localVarTypeMap = JavaMethodCallAnalysisUtils.buildLocalVarTypeMap(method);
            for (Statement stmt : body.getStatements()) {
                printStatement(stmt, level + 1, className, method, localVarTypeMap, writer);
            }
        });
    }

    /**
     * 基于方法签名构造去重键（类名.方法名(入参类型1,入参类型2,...)），用于区分重载方法。
     */
    static String buildDedupKey(String className, MethodDeclaration method) {
        StringBuilder sb = new StringBuilder(className).append(".").append(method.getNameAsString()).append("(");
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            String type = normalizeTypeName(method.getParameter(i).getType().asString());
            sb.append(type != null ? type : "?");
        }
        return sb.append(")").toString();
    }

    /**
     * 递归打印单条语句结构，并尝试继续向下展开方法调用。
     */
    static void printStatement(
            Statement stmt,
            int level,
            String className,
            MethodDeclaration ownerMethod,
            Map<String, String> localVarTypeMap,
            PrintWriter writer
    ) {
        stmt = stripComments(stmt);
        if (stmt.isIfStmt()) {
            IfStmt s = stmt.asIfStmt();
            write(writer, level, "if (" + s.getCondition() + ")");
            printStatement(s.getThenStmt(), level + 1, className, ownerMethod, localVarTypeMap, writer);

            s.getElseStmt().ifPresent(e -> {
                write(writer, level, "else");
                printStatement(e, level + 1, className, ownerMethod, localVarTypeMap, writer);
            });
            return;
        }

        if (stmt.isBlockStmt()) {
            for (Statement s : stmt.asBlockStmt().getStatements()) {
                printStatement(s, level, className, ownerMethod, localVarTypeMap, writer);
            }
            return;
        }

        if (stmt.isForStmt()) {
            write(writer, level, "for (...)");
            printStatement(stmt.asForStmt().getBody(), level + 1, className, ownerMethod, localVarTypeMap, writer);
            return;
        }

        if (stmt.isForEachStmt()) {
            write(writer, level, "for (" + clean(stmt.asForEachStmt().getVariable().toString())
                    + " : " + clean(stmt.asForEachStmt().getIterable().toString()) + ")");
            printStatement(stmt.asForEachStmt().getBody(), level + 1, className, ownerMethod, localVarTypeMap, writer);
            return;
        }

        if (stmt.isWhileStmt()) {
            write(writer, level, "while (" + stmt.asWhileStmt().getCondition() + ")");
            printStatement(stmt.asWhileStmt().getBody(), level + 1, className, ownerMethod, localVarTypeMap, writer);
            return;
        }

        if (stmt.isTryStmt()) {
            if (stmt.asTryStmt().getResources().isEmpty()) {
                write(writer, level, "try");
            } else {
                write(writer, level, "try (...)");
            }
            printStatement(stmt.asTryStmt().getTryBlock(), level + 1, className, ownerMethod, localVarTypeMap, writer);

            stmt.asTryStmt().getCatchClauses().forEach(c -> {
                write(writer, level, "catch (" + clean(c.getParameter().toString()) + ")");
                printStatement(c.getBody(), level + 1, className, ownerMethod, localVarTypeMap, writer);
            });

            stmt.asTryStmt().getFinallyBlock().ifPresent(f -> {
                write(writer, level, "finally");
                printStatement(f, level + 1, className, ownerMethod, localVarTypeMap, writer);
            });
            return;
        }

        write(writer, level, clean(stmt.toString()));

        Map<String, String> fieldMap = classFieldMap.getOrDefault(className, Collections.emptyMap());
        stmt.findAll(MethodCallExpr.class).forEach(call -> {
            String methodName = call.getNameAsString();
            int argCount = call.getArguments().size();
            List<String> argTypeHints = inferCallArgumentTypeHints(call, localVarTypeMap, fieldMap);
            LinkedHashSet<String> targets = new LinkedHashSet<>();

            if (!call.getScope().isPresent()) {
                addLocalTargetIfPresent(targets, className, methodName, argCount, argTypeHints);
            } else {
                String scopeExpr = clean(call.getScope().get().toString());
                String scopeVar = normalizeScopeVar(scopeExpr);

                if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
                    addLocalTargetIfPresent(targets, className, methodName, argCount, argTypeHints);
                }

                String fieldType = normalizeTypeName(fieldMap.get(scopeVar));
                if (fieldType != null) {
                    collectTargetsByType(targets, fieldType, methodName, argCount, argTypeHints, className, false);
                } else if (isTypeLikeScope(scopeExpr)) {
                    collectTargetsByType(targets, scopeExpr, methodName, argCount, argTypeHints, className, true);
                }
            }

            for (String targetClass : targets) {
                expand(targetClass, methodName, argCount, argTypeHints, level + 1, writer);
            }
        });
    }

    /**
     * 如果当前类中存在目标方法，则将其加入待展开集合。
     */
    static void addLocalTargetIfPresent(
            LinkedHashSet<String> targets,
            String className,
            String methodName,
            Integer argCount,
            List<String> argTypeHints
    ) {
        if (resolveMethod(className, methodName, argCount, argTypeHints) != null) {
            targets.add(className);
        }
    }

    /**
     * 根据字段类型、接口实现和静态调用范围收集候选目标类。
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
        if (type == null) {
            return;
        }

        List<String> classCandidates = resolveClassCandidates(type, contextClassName);
        if (staticTypeScope) {
            // For static-style calls like TypeName.method(...), choose a single best candidate
            // to avoid expanding same-simple-name classes from unrelated packages.
            for (String candidate : classCandidates) {
                if (resolveMethod(candidate, methodName, argCount, argTypeHints) != null) {
                    targets.add(candidate);
                    return;
                }
            }
        }

        boolean matchedInterfaceByExactType = false;
        for (String candidateType : classCandidates) {
            int before = targets.size();
            addAll(targets, interfaceToImpl.get(candidateType));
            if (targets.size() > before) {
                matchedInterfaceByExactType = true;
            }
        }
        if (!matchedInterfaceByExactType) {
            addAll(targets, interfaceToImpl.get(type));
            addAll(targets, interfaceToImpl.get(shortName(type)));
        }

        for (String candidate : classCandidates) {
            if (resolveMethod(candidate, methodName, argCount, argTypeHints) != null) {
                targets.add(candidate);
            }
        }
    }

    /**
     * 为入口类名寻找所有可能命中的全限定类名。
     */
    static List<String> resolveEntryClassCandidates(String className, String methodName) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String type = normalizeTypeName(className);
        if (type == null || type.isEmpty()) {
            return new ArrayList<>();
        }

        if (type.contains(".")) {
            if (resolveMethod(type, methodName, null, null) != null) {
                result.add(type);
            }
        } else {
            Set<String> classes = simpleNameToFullClass.get(type);
            if (classes != null) {
                for (String fullClass : classes) {
                    if (resolveMethod(fullClass, methodName, null, null) != null) {
                        result.add(fullClass);
                    }
                }
            }
        }
        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * 在重载候选中按参数个数和类型提示解析最合适的方法。
     */
    static MethodDeclaration resolveMethod(String className, String methodName, Integer argCount, List<String> argTypeHints) {
        List<MethodDeclaration> candidates = methodMap.get(className + "." + methodName);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (argCount == null) {
            for (MethodDeclaration candidate : candidates) {
                if (candidate.getParameters().isEmpty()) {
                    return candidate;
                }
            }
            return candidates.get(0);
        }

        List<MethodDeclaration> exactCandidates = new ArrayList<>();
        MethodDeclaration bestVarArgs = null;
        int bestVarArgsParamCount = -1;
        for (MethodDeclaration candidate : candidates) {
            int paramCount = candidate.getParameters().size();
            boolean varArgs = !candidate.getParameters().isEmpty()
                    && candidate.getParameter(candidate.getParameters().size() - 1).isVarArgs();
            if (!varArgs && paramCount == argCount) {
                exactCandidates.add(candidate);
                continue;
            }
            if (varArgs && argCount >= paramCount - 1 && paramCount > bestVarArgsParamCount) {
                bestVarArgs = candidate;
                bestVarArgsParamCount = paramCount;
            }
        }
        if (!exactCandidates.isEmpty()) {
            if (exactCandidates.size() == 1) {
                return exactCandidates.get(0);
            }
            MethodDeclaration best = exactCandidates.get(0);
            int bestScore = Integer.MIN_VALUE;
            for (MethodDeclaration candidate : exactCandidates) {
                int score = scoreOverloadCandidate(candidate, argTypeHints);
                if (score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
            return best;
        }
        if (bestVarArgs != null) {
            return bestVarArgs;
        }
        return null;
    }

    /**
     * 为调用参数推断粗粒度类型提示，用于重载方法匹配。
     */
    static List<String> inferCallArgumentTypeHints(
            MethodCallExpr call,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldTypeMap
    ) {
        List<String> hints = new ArrayList<>();
        for (Expression arg : call.getArguments()) {
            hints.add(inferExpressionTypeHint(arg, localVarTypeMap, fieldTypeMap));
        }
        return hints;
    }

    /**
     * 从字面量、局部变量和字段访问中推断表达式的类型提示。
     */
    static String inferExpressionTypeHint(
            Expression expr,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldTypeMap
    ) {
        if (expr == null) {
            return null;
        }
        if (expr.isNullLiteralExpr()) {
            return "null";
        }
        if (expr.isStringLiteralExpr()) {
            return "String";
        }
        if (expr.isBooleanLiteralExpr()) {
            return "boolean";
        }
        if (expr.isCharLiteralExpr()) {
            return "char";
        }
        if (expr.isIntegerLiteralExpr()) {
            return "int";
        }
        if (expr.isLongLiteralExpr()) {
            return "long";
        }
        if (expr.isDoubleLiteralExpr()) {
            return "double";
        }
        if (expr.isObjectCreationExpr()) {
            return normalizeTypeName(expr.asObjectCreationExpr().getType().asString());
        }
        if (expr.isCastExpr()) {
            return normalizeTypeName(expr.asCastExpr().getType().asString());
        }
        if (expr.isNameExpr()) {
            return normalizeTypeName(localVarTypeMap.get(expr.asNameExpr().getNameAsString()));
        }
        if (expr.isFieldAccessExpr()) {
            String scopeVar = normalizeScopeVar(expr.toString());
            return normalizeTypeName(fieldTypeMap.get(scopeVar));
        }
        return null;
    }

    /**
     * 为重载候选方法计算匹配得分。
     */
    static int scoreOverloadCandidate(MethodDeclaration candidate, List<String> argTypeHints) {
        if (argTypeHints == null || argTypeHints.isEmpty()) {
            return 0;
        }
        int score = 0;
        int count = Math.min(candidate.getParameters().size(), argTypeHints.size());
        for (int i = 0; i < count; i++) {
            String paramType = normalizeTypeName(candidate.getParameter(i).getType().asString());
            String argType = normalizeTypeName(argTypeHints.get(i));
            score += scoreTypeCompatibility(paramType, argType);
        }
        return score;
    }

    /**
     * 比较参数类型和实参类型提示的兼容程度。
     */
    static int scoreTypeCompatibility(String paramType, String argType) {
        if (argType == null || argType.isEmpty()) {
            return 0;
        }
        if ("null".equals(argType)) {
            return isPrimitiveType(paramType) ? -50 : 1;
        }
        String paramSimple = shortName(paramType);
        String argSimple = shortName(argType);
        if (paramType != null && argType != null && (paramType.equals(argType) || paramSimple.equals(argSimple))) {
            return 8;
        }
        if (isPrimitiveWrapperPair(paramSimple, argSimple)) {
            return 6;
        }
        if ("Object".equals(paramSimple)) {
            return 1;
        }
        return -4;
    }

    /**
     * 判断类型名是否是 Java 基本类型。
     */
    static boolean isPrimitiveType(String typeName) {
        if (typeName == null) {
            return false;
        }
        String t = shortName(typeName);
        return "byte".equals(t)
                || "short".equals(t)
                || "int".equals(t)
                || "long".equals(t)
                || "float".equals(t)
                || "double".equals(t)
                || "boolean".equals(t)
                || "char".equals(t);
    }

    /**
     * 判断两个类型是否属于基本类型与包装类型的互相对应关系。
     */
    static boolean isPrimitiveWrapperPair(String leftType, String rightType) {
        return ("byte".equals(leftType) && "Byte".equals(rightType))
                || ("Byte".equals(leftType) && "byte".equals(rightType))
                || ("short".equals(leftType) && "Short".equals(rightType))
                || ("Short".equals(leftType) && "short".equals(rightType))
                || ("int".equals(leftType) && "Integer".equals(rightType))
                || ("Integer".equals(leftType) && "int".equals(rightType))
                || ("long".equals(leftType) && "Long".equals(rightType))
                || ("Long".equals(leftType) && "long".equals(rightType))
                || ("float".equals(leftType) && "Float".equals(rightType))
                || ("Float".equals(leftType) && "float".equals(rightType))
                || ("double".equals(leftType) && "Double".equals(rightType))
                || ("Double".equals(leftType) && "double".equals(rightType))
                || ("boolean".equals(leftType) && "Boolean".equals(rightType))
                || ("Boolean".equals(leftType) && "boolean".equals(rightType))
                || ("char".equals(leftType) && "Character".equals(rightType))
                || ("Character".equals(leftType) && "char".equals(rightType));
    }

    /**
     * 生成用于输出的简化方法签名文本。
     */
    static String methodDisplay(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder(method.getNameAsString()).append("(");
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String type = clean(method.getParameter(i).getType().asString());
            if (method.getParameter(i).isVarArgs()) {
                sb.append(type).append("...");
            } else {
                sb.append(type);
            }
        }
        return sb.append(")").toString();
    }

    /**
     * 统计当前缓存中的方法数量，便于记录扫描结果。
     */
    static int indexedMethodCount() {
        int total = 0;
        for (List<MethodDeclaration> methods : methodMap.values()) {
            total += methods.size();
        }
        return total;
    }

    /**
     * 判断作用域表达式是否更像一个类型名而不是变量名。
     */
    static boolean isTypeLikeScope(String scope) {
        return JavaParseTextUtils.isTypeLikeScope(scope);
    }

    /**
     * 生成完整的并排 HTML diff 页面。
     */
    static void generateHtml(String fileA, String fileB, String output) throws Exception {
        List<String> left = Files.readAllLines(Paths.get(fileA));
        List<String> right = Files.readAllLines(Paths.get(fileB));
        Patch<String> patch = DiffUtils.diff(left, right);
        JavaParseLogUtils.logInfo(String.format(Locale.ROOT, "Full diff stats: left=%d, right=%d, deltas=%d",
                left.size(), right.size(), patch.getDeltas().size()));

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<style>")
                .append("body{margin:0;font-family:Menlo,Consolas,monospace;background:#fff;}")
                .append(".table-wrap{overflow:auto;max-height:100vh;}")
                .append("table{border-collapse:collapse;min-width:100%;table-layout:fixed;}")
                .append("col.ln-col{width:64px;}")
                .append("col.left-code-col{width:calc((100% - 128px) / 2);}")
                .append("col.right-code-col{width:calc((100% - 128px) / 2);}")
                .append("td{padding:2px 6px;vertical-align:top;}")
                .append(".ln{color:#999;background:#f7f7f7;text-align:right;user-select:none;}")
                .append(".code{overflow-x:auto;}")
                .append(".code .line{display:block;white-space:pre;padding-left:calc(var(--indent, 0) * 1ch);}")
                .append(".add{background:#e6ffed;}")
                .append(".del{background:#ffeef0;}")
                .append("</style></head><body>")
                .append("<div class='table-wrap'><table><colgroup>")
                .append("<col class='ln-col'><col class='left-code-col'><col class='ln-col'><col class='right-code-col'>")
                .append("</colgroup>");

        int leftNo = 1;
        int rightNo = 1;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            while (leftNo < delta.getSource().getPosition() + 1) {
                html.append(ProjectMethodCompareHtmlUtils.codeRow(leftNo, rightNo, left.get(leftNo - 1), right.get(rightNo - 1), ""));
                leftNo++;
                rightNo++;
            }

            for (String s : delta.getSource().getLines()) {
                html.append(ProjectMethodCompareHtmlUtils.codeRow(leftNo++, null, s, "", "del"));
            }

            for (String s : delta.getTarget().getLines()) {
                html.append(ProjectMethodCompareHtmlUtils.codeRow(null, rightNo++, "", s, "add"));
            }
        }

        while (leftNo <= left.size() || rightNo <= right.size()) {
            String leftText = leftNo <= left.size() ? left.get(leftNo - 1) : "";
            String rightText = rightNo <= right.size() ? right.get(rightNo - 1) : "";
            html.append(ProjectMethodCompareHtmlUtils.codeRow(
                    leftNo <= left.size() ? leftNo : null,
                    rightNo <= right.size() ? rightNo : null,
                    leftText,
                    rightText,
                    ""
            ));
            if (leftNo <= left.size()) {
                leftNo++;
            }
            if (rightNo <= right.size()) {
                rightNo++;
            }
        }

        html.append("</table></div></body></html>");
        CsvIOUtils.writeUtf8(Paths.get(output), html.toString());
        JavaParseLogUtils.logInfo("Full diff html written: " + output);
    }

    /**
     * 只保留差异片段和上下文，输出文本版 diff 日志。
     */
    static void generateDiffOnlyLog(String fileA, String fileB, String output, int contextLines) throws Exception {
        List<String> left = Files.readAllLines(Paths.get(fileA));
        List<String> right = Files.readAllLines(Paths.get(fileB));
        Patch<String> patch = DiffUtils.diff(left, right);
        JavaParseLogUtils.logInfo(String.format(Locale.ROOT, "Diff-only log stats: left=%d, right=%d, deltas=%d, context=%d",
                left.size(), right.size(), patch.getDeltas().size(), contextLines));

        List<String> lines = new ArrayList<>();
        lines.add("==== ONLY DIFFERENCES ====");
        lines.add("LEFT : " + fileA);
        lines.add("RIGHT: " + fileB);
        lines.add("");

        if (patch.getDeltas().isEmpty()) {
            lines.add("No differences.");
            CsvIOUtils.writeLines(Paths.get(output), lines);
            return;
        }

        int idx = 1;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int sourceStart = delta.getSource().getPosition() + 1;
            int targetStart = delta.getTarget().getPosition() + 1;
            ContextRange ctx = calcContextRange(left, sourceStart, delta.getSource().size(), contextLines);
            lines.add(String.format(
                    Locale.ROOT,
                    "[%d] %s | left@%d(%d) -> right@%d(%d)",
                    idx++,
                    delta.getType(),
                    sourceStart,
                    delta.getSource().size(),
                    targetStart,
                    delta.getTarget().size()
            ));

            if (ctx.beforeStart <= ctx.beforeEnd) {
                lines.add("  [context-before]");
                for (int lineNo = ctx.beforeStart; lineNo <= ctx.beforeEnd; lineNo++) {
                    lines.add(String.format(Locale.ROOT, "  %6d | %s", lineNo, left.get(lineNo - 1)));
                }
            }

            int sourceLineNo = sourceStart;
            for (String s : delta.getSource().getLines()) {
                lines.add(String.format(Locale.ROOT, "- L%5d | %s", sourceLineNo++, s));
            }
            int targetLineNo = targetStart;
            for (String s : delta.getTarget().getLines()) {
                lines.add(String.format(Locale.ROOT, "+ R%5d | %s", targetLineNo++, s));
            }

            if (ctx.afterStart <= ctx.afterEnd) {
                lines.add("  [context-after]");
                for (int lineNo = ctx.afterStart; lineNo <= ctx.afterEnd; lineNo++) {
                    lines.add(String.format(Locale.ROOT, "  %6d | %s", lineNo, left.get(lineNo - 1)));
                }
            }
            lines.add("");
        }

        CsvIOUtils.writeLines(Paths.get(output), lines);
        JavaParseLogUtils.logInfo("Diff-only log written: " + output);
    }

    /**
     * 只保留差异片段和上下文，输出 HTML 版 diff 页面。
     */
    static void generateDiffOnlyHtml(String fileA, String fileB, String output, int contextLines) throws Exception {
        List<String> left = Files.readAllLines(Paths.get(fileA));
        List<String> right = Files.readAllLines(Paths.get(fileB));
        Patch<String> patch = DiffUtils.diff(left, right);
        JavaParseLogUtils.logInfo(String.format(Locale.ROOT, "Diff-only html stats: left=%d, right=%d, deltas=%d, context=%d",
                left.size(), right.size(), patch.getDeltas().size(), contextLines));

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<style>")
                .append("body{margin:0;font-family:Menlo,Consolas,monospace;background:#fff;}")
                .append(".table-wrap{max-height:100vh;overflow-y:auto;}")
                .append(".chunk{overflow-x:auto;margin-bottom:8px;}")
                .append(".chunk table{border-collapse:collapse;min-width:100%;}")
                .append("col.ln-col{width:64px;}")
                .append("col.left-code-col{width:calc((100% - 128px) / 2);}")
                .append("col.right-code-col{width:calc((100% - 128px) / 2);}")
                .append("td{padding:2px 6px;vertical-align:top;}")
                .append(".ln{color:#999;background:#f7f7f7;text-align:right;user-select:none;}")
                .append(".code .line{display:block;white-space:pre;padding-left:calc(var(--indent, 0) * 1ch);}")
                .append(".add{background:#e6ffed;}")
                .append(".del{background:#ffeef0;}")
                .append(".ctx{background:#fafafa;}")
                .append(".sec{background:#f0f0f0;color:#444;font-weight:600;}")
                .append("</style></head><body>")
                .append("<div class='table-wrap'>");

        if (patch.getDeltas().isEmpty()) {
            html.append("<div class='chunk'><table><colgroup>")
                    .append("<col class='ln-col'><col class='left-code-col'><col class='ln-col'><col class='right-code-col'>")
                    .append("</colgroup>")
                    .append("<tr><td class='sec' colspan='4'>No differences.</td></tr>")
                    .append("</table></div>");
            html.append("</div></body></html>");
            CsvIOUtils.writeUtf8(Paths.get(output), html.toString());
            return;
        }

        int idx = 1;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int sourceStart = delta.getSource().getPosition() + 1;
            int targetStart = delta.getTarget().getPosition() + 1;
            ContextRange ctx = calcContextRange(left, sourceStart, delta.getSource().size(), contextLines);

            // 每个 CHANGE 块独立包裹，左右代码列固定 50% 宽度且各自独立滚动
            html.append("<div class='chunk'><table><colgroup>")
                    .append("<col class='ln-col'><col class='left-code-col'><col class='ln-col'><col class='right-code-col'>")
                    .append("</colgroup>");

            html.append(sectionRow(String.format(
                    Locale.ROOT,
                    "[%d] %s | left@%d(%d) -> right@%d(%d)",
                    idx++,
                    delta.getType(),
                    sourceStart,
                    delta.getSource().size(),
                    targetStart,
                    delta.getTarget().size()
            )));

            int beforeCount = ctx.beforeStart <= ctx.beforeEnd ? (ctx.beforeEnd - ctx.beforeStart + 1) : 0;
            int rightBeforeStart = Math.max(1, targetStart - beforeCount);
            for (int i = 0; i < beforeCount; i++) {
                Integer leftNo = ctx.beforeStart + i;
                Integer rightNo = rightBeforeStart + i;
                String leftText = leftNo <= left.size() ? left.get(leftNo - 1) : "";
                String rightText = rightNo <= right.size() ? right.get(rightNo - 1) : "";
                html.append(ProjectMethodCompareHtmlUtils.codeRow(leftNo, rightNo <= right.size() ? rightNo : null, leftText, rightText, "ctx"));
            }

            int sourceLineNo = sourceStart;
            for (String s : delta.getSource().getLines()) {
                html.append(ProjectMethodCompareHtmlUtils.codeRow(sourceLineNo++, null, s, "", "del"));
            }
            int targetLineNo = targetStart;
            for (String s : delta.getTarget().getLines()) {
                html.append(ProjectMethodCompareHtmlUtils.codeRow(null, targetLineNo++, "", s, "add"));
            }

            int afterCount = ctx.afterStart <= ctx.afterEnd ? (ctx.afterEnd - ctx.afterStart + 1) : 0;
            int rightAfterStart = targetStart + delta.getTarget().size();
            for (int i = 0; i < afterCount; i++) {
                Integer leftNo = ctx.afterStart + i;
                Integer rightNo = rightAfterStart + i;
                String leftText = leftNo <= left.size() ? left.get(leftNo - 1) : "";
                String rightText = rightNo <= right.size() ? right.get(rightNo - 1) : "";
                html.append(ProjectMethodCompareHtmlUtils.codeRow(leftNo, rightNo <= right.size() ? rightNo : null, leftText, rightText, "ctx"));
            }

            html.append("</table></div>");
        }

        html.append("</div></body></html>");
        CsvIOUtils.writeUtf8(Paths.get(output), html.toString());
        JavaParseLogUtils.logInfo("Diff-only html written: " + output);
    }

    /**
     * 为差异块计算前后上下文区间。
     */
    static ContextRange calcContextRange(List<String> lines, int changeStart, int changeSize, int contextLines) {
        if (lines == null || lines.isEmpty()) {
            return new ContextRange(1, 0, 1, 0);
        }

        int start = Math.max(1, Math.min(changeStart, lines.size() + 1));

        int beforeEnd = start - 1;
        int beforeStart = beforeEnd + 1;
        if (beforeEnd >= 1 && contextLines > 0) {
            beforeStart = Math.max(1, beforeEnd - contextLines + 1);
            for (int i = beforeEnd; i >= beforeStart; i--) {
                if (isMethodStartLine(lines.get(i - 1))) {
                    beforeStart = i;
                    break;
                }
            }
        }

        int changeEnd = changeSize <= 0 ? start - 1 : start + changeSize - 1;
        int afterStart = changeEnd + 1;
        int afterEnd = afterStart - 1;
        if (afterStart <= lines.size() && contextLines > 0) {
            afterEnd = Math.min(lines.size(), afterStart + contextLines - 1);
            for (int i = afterStart; i <= afterEnd; i++) {
                if (isMethodStartLine(lines.get(i - 1))) {
                    afterEnd = i - 1;
                    break;
                }
            }
        }

        return new ContextRange(beforeStart, beforeEnd, afterStart, afterEnd);
    }

    /**
     * 判断日志行是否像是一个方法标题行，用于截断上下文。
     */
    static boolean isMethodStartLine(String line) {
        String text = clean(line);
        if (text.isEmpty()) {
            return false;
        }
        if (text.contains(" ") || text.contains("(") || text.contains(")") || text.contains("=") || text.contains(";")) {
            return false;
        }
        return text.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.[A-Za-z_$][A-Za-z0-9_$]*");
    }

    /**
     * 生成差异分段标题行。
     */
    static String sectionRow(String text) {
        return "<tr><td class='sec' colspan='4'>" + ProjectMethodCompareHtmlUtils.escape(text) + "</td></tr>";
    }

    /**
     * 描述单个差异块前后上下文范围。
     */
    static class ContextRange {
        final int beforeStart;
        final int beforeEnd;
        final int afterStart;
        final int afterEnd;

        /**
         * 保存单个差异块的前后上下文边界。
         */
        ContextRange(int beforeStart, int beforeEnd, int afterStart, int afterEnd) {
            this.beforeStart = beforeStart;
            this.beforeEnd = beforeEnd;
            this.afterStart = afterStart;
            this.afterEnd = afterEnd;
        }
    }

    /**
     * 按层级向日志输出一行文本。
     */
    static void write(PrintWriter writer, int level, String text) {
        for (int i = 0; i < level; i++) {
            writer.print("    ");
        }
        writer.println(text);
    }

    /**
     * 以统一缩进输出 mapper SQL 片段。
     */
    static void writeSqlBlock(PrintWriter writer, int level, String sql) {
        write(writer, level, "[mapper-sql]");
        List<String> lines = ProjectMethodCompareSqlUtils.splitSqlLines(sql);
        if (lines.isEmpty()) {
            return;
        }

        int baseIndent = ProjectMethodCompareSqlUtils.minLeadingWhitespace(lines);
        for (String line : lines) {
            write(writer, level, ProjectMethodCompareSqlUtils.stripLeadingWhitespace(line, baseIndent));
        }
    }

    /**
     * 将语句压缩为单行文本，便于日志和 diff 展示。
     */
    static String clean(String s) {
        return JavaParseTextUtils.normalizeInlineWhitespace(s);
    }

    /**
     * 返回剥离所有 Java 注释后的语句副本，避免注释干扰对比。
     */
    static Statement stripComments(Statement stmt) {
        if (stmt == null) {
            return null;
        }
        Statement copy = stmt.clone();
        copy.getComment().ifPresent(com.github.javaparser.ast.Node::remove);
        copy.getAllContainedComments().forEach(com.github.javaparser.ast.Node::remove);
        return copy;
    }

    /**
     * 规范化作用域表达式，便于从字段映射里反查类型。
     */
    static String normalizeScopeVar(String varName) {
        return JavaParseTextUtils.normalizeScopeVar(varName);
    }

    /**
     * 去除泛型和数组标记，得到更稳定的类型名。
     */
    static String normalizeTypeName(String typeName) {
        return JavaParseTextUtils.normalizeTypeName(typeName);
    }

    /**
     * 按默认上下文解析类型名可能对应的类候选。
     */
    static List<String> resolveClassCandidates(String typeName) {
        return resolveClassCandidates(typeName, null);
    }

    /**
     * 结合 import、包名和已扫描类集合解析类型名候选。
     */
    static List<String> resolveClassCandidates(String typeName, String contextClassName) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String type = normalizeTypeName(typeName);
        if (type == null || type.isEmpty()) {
            return new ArrayList<>();
        }

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
                    if (importPkg == null || importPkg.isEmpty()) {
                        continue;
                    }
                    contextCandidates.add(importPkg + "." + simple);
                }
            }

            LinkedHashSet<String> knownContextCandidates = new LinkedHashSet<>();
            for (String candidate : contextCandidates) {
                if (isScannedClass(candidate)) {
                    knownContextCandidates.add(candidate);
                }
            }
            if (!knownContextCandidates.isEmpty()) {
                return new ArrayList<>(knownContextCandidates);
            }
            if (!contextCandidates.isEmpty()) {
                return new ArrayList<>(contextCandidates);
            }
        }

        addAll(result, simpleNameToFullClass.get(type));
        addAll(result, simpleNameToFullClass.get(simple));
        return new ArrayList<>(result);
    }

    /**
     * 判断候选全限定类名是否已被当前扫描结果收录。
     */
    static boolean isScannedClass(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return false;
        }
        Set<String> classes = simpleNameToFullClass.get(shortName(fullClassName));
        return classes != null && classes.contains(fullClassName);
    }

    /**
     * 为接口缓存实现类，避免重复写入。
     */
    static void addInterfaceImpl(String iface, String impl) {
        if (iface == null || iface.isEmpty()) {
            return;
        }
        List<String> impls = interfaceToImpl.computeIfAbsent(iface, k -> new ArrayList<>());
        if (!impls.contains(impl)) {
            impls.add(impl);
        }
    }

    /**
     * 将来源集合追加到目标集合，兼容空集合输入。
     */
    static void addAll(LinkedHashSet<String> target, Collection<String> source) {
        if (source == null) {
            return;
        }
        target.addAll(source);
    }

    /**
     * 截取全限定类名中的简单类名。
     */
    static String shortName(String s) {
        return JavaParseTextUtils.shortClassName(s);
    }

}