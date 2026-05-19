package org.ipring.jacg.controller.migrator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.ipring.jacg.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Java 代码迁移工具。
 * <p>
 * 从 {@link JavaCodeMigratorConfig} 中读取入口类名、方法名及路径配置，
 * 递归解析并遍历其调用链，精准提取接口请求流转所经过的最小代码集。
 * <p>
 * 核心规则：
 * <ul>
 *   <li><b>方法级提取</b>：仅提取调用链上实际执行的方法，剔除所在类的冗余代码</li>
 *   <li><b>目录保留</b>：严格保留原项目对应的完整包名与目录层级</li>
 *   <li><b>方法合并</b>：目标已有同名类时追加新方法，同名方法判定为冲突并原地覆盖</li>
 *   <li><b>模型/枚举保护</b>：目标项目中已存在同名类时跳过迁移（全局搜索）</li>
 *   <li><b>Mapper XML 同步</b>：复制 Mapper 方法时自动同步对应 XML 中的 SQL</li>
 *   <li><b>原格式保留</b>：禁止对迁移代码进行任何自动格式化操作</li>
 * </ul>
 * <p>
 * 使用前请在 {@link JavaCodeMigratorConfig} 中配置入口常量，然后直接运行 {@code main()}。
 *
 * @see JavaCodeMigratorConfig
 */
public final class JavaCodeMigrator {

    // ---------- 缓存/索引 ----------
    /** 全类名.方法名 -> MethodDeclaration 列表（重载） */
    private static final Map<String, List<MethodDeclaration>> methodMap = new HashMap<>();
    /** 全类名 -> Map<字段名, 字段类型> */
    private static final Map<String, Map<String, String>> classFieldMap = new HashMap<>();
    /** 全类名 -> 包名 */
    private static final Map<String, String> classPackageMap = new HashMap<>();
    /** 全类名 -> Map<简单类名, 全限定类名> （显式 import） */
    private static final Map<String, Map<String, String>> classImportTypeMap = new HashMap<>();
    /** 全类名 -> List<包名.*> （通配 import） */
    private static final Map<String, List<String>> classImportOnDemandPackageMap = new HashMap<>();
    /** 简单类名 -> Set<全限定类名> */
    private static final Map<String, Set<String>> simpleNameToFullClass = new HashMap<>();
    /** 接口全限定名 -> List<实现类全限定名> */
    private static final Map<String, List<String>> interfaceToImpl = new HashMap<>();
    /** 全类名 -> 源文件 Path */
    private static final Map<String, Path> classSourceFileMap = new HashMap<>();
    /** 全类名 -> 类声明（用于检测枚举/接口/注解种类） */
    private static final Map<String, TypeDeclaration<?>> classDeclarationMap = new HashMap<>();
    /** 扫描到的全类名集合 */
    private static final Set<String> allScannedClasses = new HashSet<>();
    /** 全类名 -> 源文件原始 CompilationUnit */
    private static final Map<String, CompilationUnit> classCompilationUnitMap = new HashMap<>();
    /** 全类名 -> 源文件原始文本（保留原始格式） */
    private static final Map<String, String> classRawText = new HashMap<>();

    // ---------- MyBatis XML 索引 ----------
    /** "namespace.methodId" -> 完整 SQL 元素文本（含标签、属性、动态子元素） */
    private static final Map<String, String> mapperSqlMap = new LinkedHashMap<>();
    /** Mapper 全限定名 -> XML 原始文本 */
    private static final Map<String, String> mapperXmlRawText = new HashMap<>();
    /** Mapper XML 中 namespace -> 源文件绝对路径 */
    private static final Map<String, Path> mapperNamespaceToFile = new HashMap<>();
    /** Mapper XML 中 namespace -> 相对于 src/main/resources/ 的路径 */
    private static final Map<String, String> mapperXmlRelPath = new HashMap<>();

    // ---------- 迁移状态 ----------
    /** 已确认需要迁移的类型全限定名 */
    private static final Set<String> migrationTypes = new LinkedHashSet<>();
    /** 已展开的方法唯一标识，避免重复展开 */
    private static final Set<String> expandedMethodKeys = new HashSet<>();
    /** 已分析过类型的类，避免循环引用 */
    private static final Set<String> analyzedTypes = new HashSet<>();
    /** 调用链中实际被使用的方法集合。key=全类名, value=方法签名集合 */
    private static final Map<String, Set<String>> usedMethods = new LinkedHashMap<>();
    /** 目标项目中已存在的全限定类名集合（全局查重，保证跨包唯一性） */
    private static final Set<String> destFullClassNames = new HashSet<>();

    // ---------- 模型类后缀 ----------
    private static final Set<String> MODEL_SUFFIXES = new HashSet<>(Arrays.asList(
            "DO", "DTO", "VO", "PO", "BO", "POJO", "Entity", "Model",
            "Req", "Resp", "Request", "Response", "Param", "Params",
            "Query", "Cmd", "Command", "Event", "Result", "Context"
    ));

    private static final List<String> SQL_TAGS = Arrays.asList("select", "insert", "update", "delete");

    // ======================== main ========================

    public static void main(String[] args) throws Exception {
        JavaParseLogUtils.logInfo("JavaCodeMigrator start.");
        JavaParseLogUtils.logInfo("  Entry class : " + JavaCodeMigratorConfig.ENTRY_CLASS_NAME);
        JavaParseLogUtils.logInfo("  Entry method: " + JavaCodeMigratorConfig.ENTRY_METHOD_NAME);
        JavaParseLogUtils.logInfo("  Source      : " + JavaCodeMigratorConfig.SOURCE_PROJECT_PATH);
        JavaParseLogUtils.logInfo("  Destination : " + JavaCodeMigratorConfig.DEST_PROJECT_PATH);

        // 0. 构建目标项目全局类名索引
        buildDestClassNameIndex();

        // 1. 扫描源项目（含 Mapper XML）
        scanSourceProject();

        // 2. 从入口方法展开调用链并收集引用类型
        expandEntryCallChain();

        // 3. 递归收集被引用类型的进一步依赖
        collectTransitiveDependencies();

        // 4. 打印汇总
        printSummary();

        // 5. 迁移代码到目标项目（含 Mapper XML 同步）
        migrateCode();

        JavaParseLogUtils.logInfo("JavaCodeMigrator finished. Migrated " + migrationTypes.size() + " types.");
    }

    // ======================== 0. 构建目标项目全局类名索引 ========================

    /**
     * 在目标项目的 src/main/java 下递归收集所有 Java 文件的全限定类名（含内部类），
     * 用于模型/枚举的全局唯一查重。全限定名保证跨包无冲突。
     */
    private static void buildDestClassNameIndex() throws IOException {
        Path destJava = Paths.get(JavaCodeMigratorConfig.DEST_PROJECT_PATH,
                JavaCodeMigratorConfig.SRC_MAIN_JAVA);
        if (!Files.exists(destJava)) {
            JavaParseLogUtils.logInfo("Destination java dir not found, skip global index: " + destJava);
            return;
        }

        try (Stream<Path> stream = Files.walk(destJava)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(JavaCodeMigratorConfig.JAVA_SUFFIX))
                    .forEach(p -> {
                        // 从路径反推包名 → 全限定类名
                        String relPath = destJava.relativize(p).toString().replace('\\', '/');
                        // 去掉 .java 后缀，将 / 替换为 .
                        String relNoExt = relPath.substring(0, relPath.length() - JavaCodeMigratorConfig.JAVA_SUFFIX.length());
                        String pkgFromPath = relNoExt.replace('/', '.');
                        // 文件对应的顶层全限定类名
                        destFullClassNames.add(pkgFromPath);

                        // 解析内部类
                        try {
                            CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                            for (TypeDeclaration<?> td : cu.getTypes()) {
                                String topName = td.getNameAsString();
                                // 顶层类全限定名（从路径推导的包名 + 类名）
                                if (!pkgFromPath.endsWith("." + topName)) {
                                    destFullClassNames.add(pkgFromPath + "." + topName);
                                }
                                // 收集内部类（Outer.Inner 形式）
                                td.findAll(ClassOrInterfaceDeclaration.class).forEach(inner ->
                                        destFullClassNames.add(pkgFromPath + "." + inner.getNameAsString()));
                                td.findAll(EnumDeclaration.class).forEach(inner ->
                                        destFullClassNames.add(pkgFromPath + "." + inner.getNameAsString()));
                            }
                        } catch (Exception ignored) {
                            // 解析失败则仅用路径推导的类名
                        }
                    });
        }
        JavaParseLogUtils.logInfo("Dest class index built: " + destFullClassNames.size() + " full names.");
    }

    // ======================== 1. 扫描源项目 ========================

    private static void scanSourceProject() throws Exception {
        long start = System.currentTimeMillis();
        File root = new File(JavaCodeMigratorConfig.SOURCE_PROJECT_PATH);
        if (!root.exists()) {
            throw new IllegalArgumentException(
                    "Source project path not found: " + JavaCodeMigratorConfig.SOURCE_PROJECT_PATH);
        }

        // 扫描 Java 源码
        File rootJava = new File(root, JavaCodeMigratorConfig.SRC_MAIN_JAVA);
        if (rootJava.exists()) scanJavaDir(rootJava);

        for (File child : JavaSourceScanUtils.listFiles(root)) {
            if (!child.isDirectory()) continue;
            File childJava = new File(child, JavaCodeMigratorConfig.SRC_MAIN_JAVA);
            if (childJava.exists()) scanJavaDir(childJava);
        }

        // 扫描 Mapper XML
        File rootResources = new File(root, "src/main/resources");
        if (rootResources.exists()) scanMapperXmlDir(rootResources);
        for (File child : JavaSourceScanUtils.listFiles(root)) {
            if (!child.isDirectory()) continue;
            File childRes = new File(child, "src/main/resources");
            if (childRes.exists()) scanMapperXmlDir(childRes);
        }

        long elapsed = System.currentTimeMillis() - start;
        JavaParseLogUtils.logInfo("Scan completed: " + allScannedClasses.size()
                + " classes, " + indexedMethodCount() + " methods, "
                + mapperSqlMap.size() + " mapper SQLs, elapsed=" + elapsed + "ms");
    }

    private static void scanJavaDir(File dir) throws Exception {
        for (File file : JavaSourceScanUtils.listFiles(dir)) {
            if (file.isDirectory()) { scanJavaDir(file); continue; }
            if (!file.getName().endsWith(JavaCodeMigratorConfig.JAVA_SUFFIX)) continue;
            processJavaFile(file);
        }
    }

    private static void processJavaFile(File file) throws Exception {
        // 读取原始文本保留格式；同时解析得到 Ranges
        String rawText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(rawText);
        } catch (Exception e) {
            JavaParseLogUtils.logWarn("Failed to parse: " + file.getAbsolutePath() + " (" + e.getMessage() + ")");
            return;
        }

        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        JavaMethodCallAnalysisUtils.ImportContext importContext =
                JavaMethodCallAnalysisUtils.buildImportContext(cu.getImports());

        Path filePath = file.toPath().toAbsolutePath().normalize();

        for (TypeDeclaration<?> typeDecl : cu.getTypes()) {
            String className = typeDecl.getNameAsString();
            String fullName = pkg.isEmpty() ? className : pkg + "." + className;

            allScannedClasses.add(fullName);
            simpleNameToFullClass.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(fullName);
            classPackageMap.put(fullName, pkg);
            classImportTypeMap.put(fullName, new HashMap<>(importContext.getExplicitImports()));
            classImportOnDemandPackageMap.put(fullName, new ArrayList<>(importContext.getOnDemandImports()));
            classSourceFileMap.put(fullName, filePath);
            classDeclarationMap.put(fullName, typeDecl);
            classCompilationUnitMap.put(fullName, cu);
            classRawText.put(fullName, rawText);

            if (typeDecl instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) typeDecl;
                clazz.getMethods().forEach(method ->
                        methodMap.computeIfAbsent(fullName + "." + method.getNameAsString(), k -> new ArrayList<>())
                                .add(method)
                );

                Map<String, String> fieldMap = new HashMap<>();
                clazz.getFields().forEach(field -> {
                    String type = field.getElementType().asString();
                    field.getVariables().forEach(var -> fieldMap.put(var.getNameAsString(), type));
                });
                classFieldMap.put(fullName, fieldMap);

                clazz.getImplementedTypes().forEach(i -> {
                    String iface = normalizeTypeName(i.getNameAsString());
                    if (iface != null && !iface.isEmpty()) {
                        addInterfaceImpl(iface, fullName);
                        addInterfaceImpl(shortName(iface), fullName);
                    }
                });
            } else if (typeDecl instanceof EnumDeclaration) {
                Map<String, String> fieldMap = new HashMap<>();
                typeDecl.getFields().forEach(field -> {
                    String type = field.getElementType().asString();
                    field.getVariables().forEach(var -> fieldMap.put(var.getNameAsString(), type));
                });
                classFieldMap.put(fullName, fieldMap);
            }
        }
    }

    // ======================== Mapper XML 扫描 ========================

    private static void scanMapperXmlDir(File resourcesRoot) {
        Path resourcesRootPath = resourcesRoot.toPath().toAbsolutePath().normalize();
        scanMapperXmlDirInternal(resourcesRoot, resourcesRootPath);
    }

    private static void scanMapperXmlDirInternal(File dir, Path resourcesRootPath) {
        for (File file : JavaSourceScanUtils.listFiles(dir)) {
            if (file.isDirectory()) { scanMapperXmlDirInternal(file, resourcesRootPath); continue; }
            if (!file.getName().endsWith(".xml")) continue;
            parseMapperXmlFile(file, resourcesRootPath);
        }
    }

    private static void parseMapperXmlFile(File file, Path resourcesRootPath) {
        try {
            String rawXml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory,
                    "http://apache.org/xml/features/disallow-doctype-decl", false);
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory,
                    "http://xml.org/sax/features/external-general-entities", false);
            ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory,
                    "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void fatalError(SAXParseException e) { /* ignore non-mybatis xml */ }
            });

            Document doc = builder.parse(new InputSource(new StringReader(rawXml)));
            Element root = doc.getDocumentElement();
            if (root == null || !"mapper".equals(root.getTagName())) return;

            String namespace = cleanXml(root.getAttribute("namespace"));
            if (namespace.isEmpty()) return;

            Path filePath = file.toPath().toAbsolutePath().normalize();
            mapperNamespaceToFile.put(namespace, filePath);
            mapperXmlRawText.put(namespace, rawXml);
            // ★ 计算 XML 文件相对于 src/main/resources/ 的实际路径
            String relPath = resourcesRootPath.relativize(filePath).toString().replace('\\', '/');
            mapperXmlRelPath.put(namespace, relPath);

            for (String tag : SQL_TAGS) {
                NodeList nodes = root.getElementsByTagName(tag);
                for (int i = 0; i < nodes.getLength(); i++) {
                    org.w3c.dom.Node n = nodes.item(i);
                    if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                    Element stmt = (Element) n;
                    String id = cleanXml(stmt.getAttribute("id"));
                    if (id.isEmpty()) continue;

                    // ★ 完整序列化整个 SQL 元素（含 resultType/parameterType 等所有属性及动态标签）
                    StringBuilder sqlBuf = new StringBuilder();
                    serializeSingleElement(stmt, sqlBuf);
                    String sqlText = sqlBuf.toString();
                    if (sqlText.trim().isEmpty()) continue;

                    String key = namespace + "." + id;
                    mapperSqlMap.put(key, sqlText);
                }
            }
        } catch (Exception ignore) {
            // 忽略非 MyBatis XML 或格式不正确的文件
        }
    }

    /**
     * 递归序列化单个 XML Element 及其完整子树（含所有属性和子节点）。
     * 保证 MyBatis 动态标签（foreach/if/where/trim/choose/set/bind 等）
     * 及 resultType/parameterType 等属性完整无损。
     */
    private static void serializeSingleElement(Element element, StringBuilder out) {
        String tagName = element.getTagName();
        out.append("<").append(tagName);
        // 输出所有属性
        org.w3c.dom.NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            org.w3c.dom.Node attr = attrs.item(i);
            out.append(" ").append(attr.getNodeName()).append("=\"")
               .append(attr.getNodeValue()).append("\"");
        }
        out.append(">");

        // 递归处理子节点
        org.w3c.dom.NodeList children = element.getChildNodes();
        boolean hasChildElements = false;
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                hasChildElements = true;
                out.append("\n");
                serializeSingleElement((Element) child, out);
            } else if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                out.append(child.getNodeValue());
            } else if (child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                out.append("<![CDATA[").append(child.getNodeValue()).append("]]>");
            }
        }

        if (hasChildElements) {
            out.append("\n");
        }
        out.append("</").append(tagName).append(">");
    }

    // ======================== 2. 展开调用链 ========================

    private static void expandEntryCallChain() {
        String entryCls = JavaCodeMigratorConfig.ENTRY_CLASS_NAME;
        List<String> candidates = resolveEntryClassCandidates(entryCls);
        if (candidates.isEmpty()) {
            JavaParseLogUtils.logWarn("Entry class not found in source project: " + entryCls);
            return;
        }
        if (candidates.size() > 1) {
            JavaParseLogUtils.logInfo("Multiple class candidates found for '" + entryCls + "': " + candidates);
        }
        for (String fullClass : candidates) {
            addMigrationType(fullClass);
            analyzeClassTypeReferences(fullClass);
            expandMethodCallChain(fullClass, JavaCodeMigratorConfig.ENTRY_METHOD_NAME, null, null, 0);
        }
    }

    private static void expandMethodCallChain(
            String className, String methodName,
            Integer argCount, List<String> argTypeHints, int level) {

        if (level > JavaCodeMigratorConfig.MAX_EXPAND_LEVEL) return;

        String methodKey = className + "." + methodName;
        MethodDeclaration method = resolveMethod(className, methodName, argCount, argTypeHints);
        if (method == null) return;

        String methodIdentityKey = methodKey + methodSignature(method);
        if (expandedMethodKeys.contains(methodIdentityKey)) return;
        expandedMethodKeys.add(methodIdentityKey);

        recordUsedMethod(className, method);
        collectMethodSignatureTypes(method);

        if (!method.getBody().isPresent()) {
            expandImplementations(className, methodName, argCount, argTypeHints, level);
            return;
        }

        Map<String, String> localVarTypeMap = JavaMethodCallAnalysisUtils.buildLocalVarTypeMap(method);
        Map<String, String> fieldMap = classFieldMap.getOrDefault(className, Collections.emptyMap());

        method.getBody().ifPresent(body -> {
            collectTypeReferencesFromNode(body, className, localVarTypeMap, fieldMap);
            for (Statement stmt : body.getStatements()) {
                expandStatement(stmt, className, localVarTypeMap, fieldMap, level);
            }
        });
    }

    private static void recordUsedMethod(String fullClassName, MethodDeclaration method) {
        usedMethods.computeIfAbsent(fullClassName, k -> new LinkedHashSet<>())
                .add(methodSignature(method));
    }

    private static void expandImplementations(
            String className, String methodName,
            Integer argCount, List<String> argTypeHints, int level) {

        List<String> impls = new ArrayList<>();
        List<String> exactImpls = interfaceToImpl.get(className);
        if (exactImpls != null) impls.addAll(exactImpls);
        List<String> simpleImpls = interfaceToImpl.get(shortName(className));
        if (simpleImpls != null) {
            for (String impl : simpleImpls) {
                if (!impls.contains(impl)) impls.add(impl);
            }
        }
        for (String impl : impls) {
            addMigrationType(impl);
            analyzeClassTypeReferences(impl);
            expandMethodCallChain(impl, methodName, argCount, argTypeHints, level + 1);
        }
    }

    private static void expandStatement(
            Statement stmt, String className,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap, int level) {

        if (stmt.isIfStmt()) {
            IfStmt s = stmt.asIfStmt();
            expandStatement(s.getThenStmt(), className, localVarTypeMap, fieldMap, level);
            s.getElseStmt().ifPresent(e -> expandStatement(e, className, localVarTypeMap, fieldMap, level));
            return;
        }
        if (stmt.isBlockStmt()) {
            stmt.asBlockStmt().getStatements().forEach(s ->
                    expandStatement(s, className, localVarTypeMap, fieldMap, level));
            return;
        }
        if (stmt.isForStmt()) {
            expandStatement(stmt.asForStmt().getBody(), className, localVarTypeMap, fieldMap, level);
            return;
        }
        if (stmt.isForEachStmt()) {
            expandStatement(stmt.asForEachStmt().getBody(), className, localVarTypeMap, fieldMap, level);
            return;
        }
        if (stmt.isWhileStmt()) {
            expandStatement(stmt.asWhileStmt().getBody(), className, localVarTypeMap, fieldMap, level);
            return;
        }
        if (stmt.isTryStmt()) {
            TryStmt tryStmt = stmt.asTryStmt();
            expandStatement(tryStmt.getTryBlock(), className, localVarTypeMap, fieldMap, level);
            tryStmt.getCatchClauses().forEach(c ->
                    expandStatement(c.getBody(), className, localVarTypeMap, fieldMap, level));
            tryStmt.getFinallyBlock().ifPresent(f ->
                    expandStatement(f, className, localVarTypeMap, fieldMap, level));
            return;
        }

        stmt.findAll(MethodCallExpr.class).forEach(call -> {
            String calledMethodName = call.getNameAsString();
            int callArgCount = call.getArguments().size();
            List<String> callArgHints = inferCallArgTypeHints(call, localVarTypeMap, fieldMap);
            LinkedHashSet<String> targets = new LinkedHashSet<>();

            if (!call.getScope().isPresent()) {
                if (resolveMethod(className, calledMethodName, callArgCount, callArgHints) != null) {
                    targets.add(className);
                }
            } else {
                String scopeExpr = cleanExpr(call.getScope().get().toString());
                String scopeVar = normalizeScopeVar(scopeExpr);
                if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
                    if (resolveMethod(className, calledMethodName, callArgCount, callArgHints) != null) {
                        targets.add(className);
                    }
                }
                String fieldType = normalizeTypeName(fieldMap.get(scopeVar));
                if (fieldType != null) {
                    collectTargetsByType(targets, fieldType, calledMethodName, callArgCount, callArgHints, className);
                } else if (isTypeLikeScope(scopeExpr)) {
                    collectTargetsByType(targets, scopeExpr, calledMethodName, callArgCount, callArgHints, className);
                }
            }

            for (String targetClass : targets) {
                addMigrationType(targetClass);
                analyzeClassTypeReferences(targetClass);
                expandMethodCallChain(targetClass, calledMethodName, callArgCount, callArgHints, level + 1);
            }
        });
    }

    // ======================== 3. 收集类型引用 ========================

    private static void collectMethodSignatureTypes(MethodDeclaration method) {
        collectTypeReference(method.getType(), null, null);
        for (Parameter param : method.getParameters()) collectTypeReference(param.getType(), null, null);
        method.getThrownExceptions().forEach(t -> collectTypeReference(t, null, null));
    }

    private static void collectTypeReferencesFromNode(
            Node node, String contextClassName,
            Map<String, String> localVarTypeMap, Map<String, String> fieldMap) {

        node.findAll(VariableDeclarator.class).forEach(var ->
                collectTypeName(var.getType().asString(), contextClassName));
        node.findAll(ObjectCreationExpr.class).forEach(expr ->
                collectTypeReference(expr.getType(), contextClassName, null));
        node.findAll(CastExpr.class).forEach(expr ->
                collectTypeReference(expr.getType(), contextClassName, null));
        node.findAll(ClassExpr.class).forEach(expr ->
                collectTypeName(expr.getType().asString(), contextClassName));
        node.findAll(InstanceOfExpr.class).forEach(expr ->
                collectTypeReference(expr.getType(), contextClassName, null));
        node.findAll(AnnotationExpr.class).forEach(ann ->
                collectTypeName(ann.getNameAsString(), contextClassName));
        node.findAll(FieldAccessExpr.class).forEach(fa -> {
            String scopeStr = fa.getScope().toString();
            if (isTypeLikeScope(scopeStr)) collectTypeName(scopeStr, contextClassName);
        });
        node.findAll(MethodCallExpr.class).forEach(call -> {
            for (Expression arg : call.getArguments()) {
                if (arg.isObjectCreationExpr())
                    collectTypeReference(arg.asObjectCreationExpr().getType(), contextClassName, null);
            }
        });
        node.findAll(SwitchStmt.class).forEach(sw -> {
            if (sw.getSelector() instanceof FieldAccessExpr) {
                String scopeStr = ((FieldAccessExpr) sw.getSelector()).getScope().toString();
                if (isTypeLikeScope(scopeStr)) collectTypeName(scopeStr, contextClassName);
            }
        });
    }

    private static void collectTypeReference(Type type, String contextClassName, Map<String, String> lvtm) {
        if (type == null) return;
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType cit = type.asClassOrInterfaceType();
            collectTypeName(cit.getNameAsString(), contextClassName);
            cit.getTypeArguments().ifPresent(args -> args.forEach(a -> collectTypeReference(a, contextClassName, lvtm)));
            cit.getScope().ifPresent(s -> collectTypeReference(s, contextClassName, lvtm));
        }
    }

    private static void collectTypeName(String typeName, String contextClassName) {
        String normalized = normalizeTypeName(typeName);
        if (normalized == null || normalized.isEmpty() || isPrimitiveType(normalized)
                || isJdkOrThirdPartyType(normalized)) return;
        if (normalized.contains(".")) {
            addMigrationType(normalized);
        } else {
            for (String r : resolveClassCandidates(normalized, contextClassName)) {
                if (!isJdkOrThirdPartyType(r)) addMigrationType(r);
            }
        }
    }

    private static void analyzeClassTypeReferences(String fullClassName) {
        if (analyzedTypes.contains(fullClassName)) return;
        analyzedTypes.add(fullClassName);
        TypeDeclaration<?> decl = classDeclarationMap.get(fullClassName);
        if (decl == null) return;

        Map<String, String> fields = classFieldMap.get(fullClassName);
        if (fields != null) {
            for (String ft : fields.values()) collectTypeName(ft, fullClassName);
        }
        if (decl instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) decl;
            clazz.getExtendedTypes().forEach(t -> collectTypeReference(t, fullClassName, null));
            clazz.getImplementedTypes().forEach(t -> collectTypeReference(t, fullClassName, null));
            clazz.getAnnotations().forEach(ann -> collectTypeName(ann.getNameAsString(), fullClassName));
            clazz.getTypeParameters().forEach(tp ->
                    tp.getTypeBound().forEach(b -> collectTypeReference(b, fullClassName, null)));
        } else if (decl instanceof EnumDeclaration) {
            ((EnumDeclaration) decl).getImplementedTypes()
                    .forEach(t -> collectTypeReference(t, fullClassName, null));
        }
    }

    private static void addMigrationType(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) return;
        if (isJdkOrThirdPartyType(fullClassName)) return;
        if (!allScannedClasses.contains(fullClassName) && !classSourceFileMap.containsKey(fullClassName)) return;
        if (migrationTypes.add(fullClassName)) analyzeClassTypeReferences(fullClassName);
    }

    // ======================== 4. 传递依赖收集 ========================

    private static void collectTransitiveDependencies() {
        int prevSize;
        do {
            prevSize = migrationTypes.size();
            List<String> snapshot = new ArrayList<>(migrationTypes);
            for (String type : snapshot) analyzeClassTypeReferences(type);
        } while (migrationTypes.size() > prevSize);
    }

    // ======================== 5. 迁移代码 ========================

    private static void migrateCode() throws IOException {
        int modelSkipped = 0, modelCopied = 0;
        int methodMerged = 0, methodNewFile = 0;
        int xmlSynced = 0, failed = 0;
        int callChainSkipped = 0;

        for (String fullClassName : migrationTypes) {
            Path srcFilePath = classSourceFileMap.get(fullClassName);
            if (srcFilePath == null) { failed++; continue; }

            String pkg = classPackageMap.get(fullClassName);
            if (pkg == null || pkg.isEmpty()) { failed++; continue; }

            String simpleName = shortName(fullClassName);
            Path destDir = Paths.get(JavaCodeMigratorConfig.DEST_PROJECT_PATH,
                    JavaCodeMigratorConfig.SRC_MAIN_JAVA, pkg.replace('.', '/'));
            Path destFile = destDir.resolve(simpleName + JavaCodeMigratorConfig.JAVA_SUFFIX);

            // 模型/枚举：全局查重后整体复制或跳过（全限定名保证跨包唯一性）
            if (isModelOrEnum(fullClassName)) {
                if (destFullClassNames.contains(fullClassName)) {
                    JavaParseLogUtils.logInfo("SKIP (model/enum, global duplicate): " + fullClassName);
                    modelSkipped++;
                } else {
                    Files.createDirectories(destDir);
                    Files.copy(srcFilePath, destFile, StandardCopyOption.REPLACE_EXISTING);
                    JavaParseLogUtils.logInfo("COPY (model/enum): " + relativeDestPath(destFile));
                    modelCopied++;
                }
                continue;
            }

            // 非模型/枚举：仅迁移调用链中的方法
            Set<String> methods = usedMethods.get(fullClassName);
            if (methods == null || methods.isEmpty()) {
                JavaParseLogUtils.logInfo("SKIP (not in call chain): " + simpleName);
                callChainSkipped++;
                continue;
            }

            Files.createDirectories(destDir);

            if (Files.exists(destFile)) {
                mergeMethodsToExisting(fullClassName, srcFilePath, methods, pkg, destFile);
                methodMerged++;
            } else {
                writeSkeletonClass(fullClassName, methods, pkg, destFile);
                methodNewFile++;
            }

            // ★ Mapper XML 同步
            if (isMapperClass(fullClassName)) {
                int synced = syncMapperXml(fullClassName, methods);
                xmlSynced += synced;
            }
        }

        JavaParseLogUtils.logInfo("Migration completed: "
                + "modelCopied=" + modelCopied + ", modelSkipped=" + modelSkipped
                + ", methodNewFile=" + methodNewFile + ", methodMerged=" + methodMerged
                + ", callChainSkipped=" + callChainSkipped
                + ", xmlSynced=" + xmlSynced + ", failed=" + failed);
    }

    // ======================== 模型/枚举判定 ========================

    private static boolean isModelOrEnum(String fullClassName) {
        TypeDeclaration<?> decl = classDeclarationMap.get(fullClassName);
        if (decl == null) return false;
        if (decl.isEnumDeclaration()) return true;
        if (decl instanceof ClassOrInterfaceDeclaration
                && ((ClassOrInterfaceDeclaration) decl).isInterface()) return false;

        String simpleName = shortName(fullClassName);
        for (String suffix : MODEL_SUFFIXES) {
            if (simpleName.endsWith(suffix)) return true;
        }
        if (decl instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) decl;
            boolean hasModelAnno = false, hasServiceAnno = false;
            for (AnnotationExpr ann : clazz.getAnnotations()) {
                String n = ann.getNameAsString();
                if (n.matches("Data|Getter|Setter|Builder|Value|NoArgsConstructor|AllArgsConstructor|ApiModel|TableName|Table")) {
                    hasModelAnno = true;
                }
                if (n.matches("Service|Component|Repository|Controller|RestController|Configuration")) {
                    hasServiceAnno = true;
                }
            }
            if (hasModelAnno && !hasServiceAnno) return true;
        }
        return false;
    }

    /**
     * 判断类是否为 MyBatis Mapper 接口。
     */
    private static boolean isMapperClass(String fullClassName) {
        TypeDeclaration<?> decl = classDeclarationMap.get(fullClassName);
        if (decl == null) return false;
        // 检查 @Mapper 注解
        for (AnnotationExpr ann : decl.getAnnotations()) {
            if ("Mapper".equals(ann.getNameAsString())) return true;
        }
        // 检查类名后缀
        return shortName(fullClassName).endsWith("Mapper");
    }

    // ======================== 方法级提取与合并（保留原始格式） ========================

    /**
     * 纯净构建：只提取调用链方法 + 字段，从头组建新文件，绝不"整体复制再删除"。
     */
    private static void writeSkeletonClass(
            String fullClassName, Set<String> methodSignatures, String pkg, Path destFile) throws IOException {

        String rawSrc = classRawText.get(fullClassName);
        if (rawSrc == null) {
            Files.copy(classSourceFileMap.get(fullClassName), destFile);
            return;
        }

        TypeDeclaration<?> typeDecl = classDeclarationMap.get(fullClassName);
        if (!(typeDecl instanceof ClassOrInterfaceDeclaration)) {
            Files.write(destFile, rawSrc.getBytes(StandardCharsets.UTF_8));
            return;
        }
        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) typeDecl;

        // 1. 提取类头：package + imports + 注解 + 类声明 + {
        int classStart = lineColToOffset(rawSrc, typeDecl.getRange().get().begin.line,
                typeDecl.getRange().get().begin.column);
        int brace = rawSrc.indexOf('{', classStart);
        if (brace < 0) {
            Files.write(destFile, rawSrc.getBytes(StandardCharsets.UTF_8));
            return;
        }
        String header = rawSrc.substring(0, brace + 1); // 含 {

        // 2. 提取字段（全部保留，因为被调用方法可能使用）
        StringBuilder fieldsBlock = new StringBuilder();
        for (BodyDeclaration<?> member : clazz.getMembers()) {
            if (member.isFieldDeclaration()) {
                String fieldText = extractNodeRawText(member, rawSrc);
                if (fieldText != null) fieldsBlock.append("\n").append(fieldText);
            }
        }

        // 3. 提取调用链中使用的方法（纯净提取，不含任何多余字符）
        StringBuilder methodsBlock = new StringBuilder();
        for (MethodDeclaration m : clazz.getMethods()) {
            if (!methodSignatures.contains(methodSignature(m))) continue;
            String methodText = extractNodeRawText(m, rawSrc);
            if (methodText != null) methodsBlock.append("\n\n").append(methodText);
        }

        // 4. 组装：header + 字段 + 方法 + }
        String result = header + fieldsBlock + methodsBlock + "\n}\n";

        // ★ 括号校验
        String braceErr = validateBraces(result, "NEW " + shortName(fullClassName));
        if (braceErr != null) {
            JavaParseLogUtils.logWarn("BRACE MISMATCH, fallback to raw copy: " + braceErr);
            Files.write(destFile, rawSrc.getBytes(StandardCharsets.UTF_8));
        } else {
            Files.write(destFile, result.getBytes(StandardCharsets.UTF_8));
        }
        JavaParseLogUtils.logInfo("NEW (build, " + methodSignatures.size() + " methods): "
                + relativeDestPath(destFile));
    }

    /**
     * 将源方法合并到目标文件。解析一次获取 Range，然后直接操作原始文本。
     */
    private static void mergeMethodsToExisting(
            String fullClassName, Path srcFilePath,
            Set<String> newMethodSignatures, String pkg, Path destFile) throws IOException {

        String destText;
        try {
            destText = new String(Files.readAllBytes(destFile), StandardCharsets.UTF_8);
        } catch (Exception e) {
            JavaParseLogUtils.logWarn("Failed to read dest file, creating new: " + destFile);
            writeSkeletonClass(fullClassName, newMethodSignatures, pkg, destFile);
            return;
        }

        CompilationUnit destCu;
        try {
            destCu = StaticJavaParser.parse(destText);
        } catch (Exception e) {
            JavaParseLogUtils.logWarn("Failed to parse dest, overwriting: " + destFile);
            writeSkeletonClass(fullClassName, newMethodSignatures, pkg, destFile);
            return;
        }

        // 源方法文本（从原始源文件提取）
        String srcRaw = classRawText.get(fullClassName);
        CompilationUnit srcCu = classCompilationUnitMap.get(fullClassName);

        TypeDeclaration<?> srcType = findTypeDeclaration(srcCu, fullClassName, pkg);
        TypeDeclaration<?> destType = findTypeDeclaration(destCu, fullClassName, pkg);
        if (!(destType instanceof ClassOrInterfaceDeclaration) || !(srcType instanceof ClassOrInterfaceDeclaration)) {
            JavaParseLogUtils.logWarn("Type mismatch, skip merge: " + fullClassName);
            return;
        }
        ClassOrInterfaceDeclaration destClazz = (ClassOrInterfaceDeclaration) destType;
        ClassOrInterfaceDeclaration srcClazz = (ClassOrInterfaceDeclaration) srcType;

        int added = 0, overwritten = 0;

        // ---- 第一步：收集并应用覆盖操作（同名方法替换） ----
        List<TextEdit> replaceEdits = new ArrayList<>();
        List<String> newMethodTexts = new ArrayList<>();

        for (MethodDeclaration srcMethod : srcClazz.getMethods()) {
            String sig = methodSignature(srcMethod);
            if (!newMethodSignatures.contains(sig)) continue;

            String srcMethodText = extractNodeRawText(srcMethod, srcRaw);
            if (srcMethodText == null) continue;

            MethodDeclaration existMethod = findMethodBySignature(destClazz, sig);
            if (existMethod != null && existMethod.getRange().isPresent()) {
                replaceEdits.add(new TextEdit(existMethod.getRange().get(), srcMethodText));
                overwritten++;
            } else {
                newMethodTexts.add(srcMethodText);
                added++;
            }
        }

        // 应用覆盖
        if (!replaceEdits.isEmpty()) {
            destText = applyEdits(destText, replaceEdits);
        }

        // ---- 第二步：新增方法插入到类 } 之前 ----
        if (!newMethodTexts.isEmpty()) {
            // 重新解析修改后的文本
            CompilationUnit updatedCu = StaticJavaParser.parse(destText);
            TypeDeclaration<?> updatedType = findTypeDeclaration(updatedCu, fullClassName, pkg);

            // ★ 找类 } 的字符偏移，直接在此处之前插入，确保 } 位置不变
            int insertOffset = findClassBodyEnd(destText,
                    updatedType != null ? updatedType : destType);
            if (insertOffset < 0) {
                insertOffset = destText.length();
            }

            StringBuilder insertBlock = new StringBuilder();
            for (String methodText : newMethodTexts) {
                insertBlock.append("\n\n    ").append(methodText.replace("\n", "\n    "));
            }
            // 在类 } 之前插入
            destText = destText.substring(0, insertOffset) + insertBlock + destText.substring(insertOffset);
        }

        // ★ 括号校验
        String braceErr = validateBraces(destText, "MERGE " + shortName(fullClassName));
        if (braceErr != null) {
            JavaParseLogUtils.logWarn("BRACE MISMATCH, merge aborted: " + braceErr);
            return; // 不写坏文件
        }
        Files.write(destFile, destText.getBytes(StandardCharsets.UTF_8));
        JavaParseLogUtils.logInfo("MERGE (added=" + added + ", overwritten=" + overwritten + "): "
                + relativeDestPath(destFile));
    }

    // ---------- 原始文本操作辅助方法 ----------

    /** 描述一次文本编辑操作 */
    private static class TextEdit {
        final com.github.javaparser.Range range;
        final String newText;

        TextEdit(com.github.javaparser.Range range, String newText) {
            this.range = range;
            this.newText = newText;
        }
    }

    // ======================== 括号平衡校验 ========================

    /**
     * 基于栈的括号匹配校验。遍历文本中所有 { } ( ) [ ]，
     * 遇到左括号压栈，遇到右括号弹栈比对，遍历结束栈为空则闭环。
     *
     * @return null 表示校验通过；否则返回错误描述字符串
     */
    private static String validateBraces(String code, String context) {
        Deque<BraceFrame> stack = new ArrayDeque<>();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            switch (c) {
                case '{': case '(': case '[':
                    stack.push(new BraceFrame(c, i));
                    break;
                case '}':
                    if (stack.isEmpty() || stack.pop().ch != '{') {
                        return String.format("[%s] 多余的 '%c'，位置 %d（附近: ...%s...）",
                                context, c, i, snippet(code, i));
                    }
                    break;
                case ')':
                    if (stack.isEmpty() || stack.pop().ch != '(') {
                        return String.format("[%s] 多余的 '%c'，位置 %d（附近: ...%s...）",
                                context, c, i, snippet(code, i));
                    }
                    break;
                case ']':
                    if (stack.isEmpty() || stack.pop().ch != '[') {
                        return String.format("[%s] 多余的 '%c'，位置 %d（附近: ...%s...）",
                                context, c, i, snippet(code, i));
                    }
                    break;
            }
        }
        if (!stack.isEmpty()) {
            BraceFrame f = stack.peek();
            return String.format("[%s] 未闭合的 '%c'，位置 %d，栈深度 %d",
                    context, f.ch, f.pos, stack.size());
        }
        return null; // 校验通过
    }

    private static class BraceFrame {
        final char ch;
        final int pos;
        BraceFrame(char ch, int pos) { this.ch = ch; this.pos = pos; }
    }

    private static String snippet(String code, int pos) {
        int s = Math.max(0, pos - 30);
        int e = Math.min(code.length(), pos + 30);
        return code.substring(s, e).replace('\n', ' ');
    }

    /**
     * 从原始文本中按 Range 提取节点文本。
     */
    private static String extractNodeRawText(Node node, String rawSrc) {
        if (node == null || !node.getRange().isPresent()) return null;
        return extractRangeText(rawSrc, node.getRange().get());
    }

    private static String extractRangeText(String rawSrc, com.github.javaparser.Range range) {
        int startOffset = lineColToOffset(rawSrc, range.begin.line, range.begin.column);
        // Range.end 是排他边界，lineColToOffset 返回该位置偏移，substring(start, end) 自动排他，刚好包含最后一个字符
        int endOffset = lineColToOffset(rawSrc, range.end.line, range.end.column);
        if (startOffset < 0 || endOffset < 0 || startOffset > endOffset) return null;
        return rawSrc.substring(startOffset, Math.min(endOffset, rawSrc.length()));
    }

    /**
     * 从底向上应用多个编辑操作到原始文本。按起始偏移降序保证前方位置不受后方编辑影响。
     */
    private static String applyEdits(String rawSrc, List<TextEdit> edits) {
        if (edits.isEmpty()) return rawSrc;
        // 按操作起始位置（字符偏移）降序排序
        List<TextEdit> sorted = new ArrayList<>(edits);
        sorted.sort((a, b) -> {
            int sa = lineColToOffset(rawSrc, a.range.begin.line, a.range.begin.column);
            int sb = lineColToOffset(rawSrc, b.range.begin.line, b.range.begin.column);
            if (sa < 0) sa = 0;
            if (sb < 0) sb = 0;
            return Integer.compare(sb, sa);
        });

        String result = rawSrc;
        for (TextEdit edit : sorted) {
            int s = lineColToOffset(result, edit.range.begin.line, edit.range.begin.column);
            int e = lineColToOffset(result, edit.range.end.line, edit.range.end.column);
            if (s < 0) continue;
            if (e < s) e = s; // 插入模式（零宽度范围）
            result = result.substring(0, s) + edit.newText + result.substring(Math.min(e, result.length()));
        }
        return result;
    }

    /**
     * 将行号/列号（均为 1-based）转换为原始文本中的字符偏移量（0-based）。
     * 返回 -1 表示越界。
     */
    private static int lineColToOffset(String text, int line, int col) {
        if (text == null || line < 1 || col < 1) return -1;
        int offset = 0;
        int currentLine = 1;
        while (offset < text.length() && currentLine < line) {
            if (text.charAt(offset) == '\n') currentLine++;
            offset++;
        }
        if (currentLine != line) return -1; // 行号超出范围
        int nxt = nextNewline(text, offset);
        int maxCol = nxt - offset; // 该行字符数（不含 \n）
        int actualCol = Math.min(col - 1, maxCol);
        return offset + actualCol;
    }

    private static int nextNewline(String text, int from) {
        int idx = text.indexOf('\n', from);
        return idx < 0 ? text.length() : idx;
    }

    /**
     * 查找目标类体闭合花括号 } 的字符偏移量。
     * 利用 JavaParser 的类声明范围精确定位，避免嵌套类或字符串中 } 的干扰。
     */
    private static int findClassBodyEnd(String text, TypeDeclaration<?> typeDecl) {
        if (typeDecl.getRange().isPresent()) {
            // 限定搜索范围在类型声明结束位置的上一行内
            int scopeEnd = lineColToOffset(text, typeDecl.getRange().get().end.line,
                    typeDecl.getRange().get().end.column);
            if (scopeEnd > 0) {
                for (int i = scopeEnd - 1; i >= 0; i--) {
                    if (text.charAt(i) == '}') return i;
                }
            }
        }
        return text.lastIndexOf('}');
    }

    // ======================== Mapper XML 同步 ========================

    /**
     * 将 Mapper 接口中使用的方法对应的 SQL 同步到目标项目的对应 XML 文件中。
     * 返回同步的 SQL 数量。
     */
    private static int syncMapperXml(String fullClassName, Set<String> methodSignatures) throws IOException {
        Path xmlSrcPath = mapperNamespaceToFile.get(fullClassName);
        if (xmlSrcPath == null) return 0;

        // ★ 使用 XML 在原 resources 下的实际相对路径，而非从 Java 包名反推
        String xmlRelPath = mapperXmlRelPath.get(fullClassName);
        if (xmlRelPath == null) return 0;

        Path destXmlFile = Paths.get(JavaCodeMigratorConfig.DEST_PROJECT_PATH,
                "src/main/resources", xmlRelPath);
        Path destXmlDir = destXmlFile.getParent();

        int synced = 0;
        Files.createDirectories(destXmlDir);

        if (!Files.exists(destXmlFile)) {
            // 目标无此 XML → 创建仅含使用 SQL 的骨架 XML
            String rawXml = mapperXmlRawText.get(fullClassName);
            if (rawXml == null) {
                Files.copy(xmlSrcPath, destXmlFile);
                JavaParseLogUtils.logInfo("XML COPY: " + xmlRelPath);
                return methodSignatures.size();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
              .append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" ")
              .append("\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n")
              .append("<mapper namespace=\"").append(fullClassName).append("\">\n");

            for (String sig : methodSignatures) {
                String methodId = extractMethodIdFromSignature(sig);
                String key = fullClassName + "." + methodId;
                String sql = mapperSqlMap.get(key);
                if (sql != null) {
                    // sql 已是完整的 <select id="x" resultType="y">...<foreach>...</foreach>...</select>
                    sb.append("\n    ").append(sql.replace("\n", "\n    ")).append("\n");
                    synced++;
                }
            }
            sb.append("</mapper>\n");
            Files.write(destXmlFile, sb.toString().getBytes(StandardCharsets.UTF_8));
            JavaParseLogUtils.logInfo("XML NEW (" + synced + " SQLs): " + xmlRelPath);
        } else {
            // 目标已有 XML → 合并 SQL
            String destXml = new String(Files.readAllBytes(destXmlFile), StandardCharsets.UTF_8);

            for (String sig : methodSignatures) {
                String methodId = extractMethodIdFromSignature(sig);
                String key = fullClassName + "." + methodId;
                String sql = mapperSqlMap.get(key);
                if (sql == null) continue;

                // 检查目标 XML 中是否已有同 id 的 SQL
                if (destXml.contains("id=\"" + methodId + "\"")) continue;

                // sql 已是完整的 <select id="x" resultType="y">...<foreach>...</foreach>...</select>
                // 在 </mapper> 之前插入
                String indentedSql = "\n    " + sql.replace("\n", "\n    ") + "\n";
                int mapperEnd = destXml.lastIndexOf("</mapper>");
                if (mapperEnd >= 0) {
                    destXml = destXml.substring(0, mapperEnd) + indentedSql + destXml.substring(mapperEnd);
                } else {
                    destXml += indentedSql;
                }
                synced++;
            }

            Files.write(destXmlFile, destXml.getBytes(StandardCharsets.UTF_8));
            if (synced > 0) {
                JavaParseLogUtils.logInfo("XML MERGE (" + synced + " SQLs): " + xmlRelPath);
            }
        }
        return synced;
    }

    /**
     * 从方法签名 "methodName(paramType1,paramType2)" 中提取方法名。
     */
    private static String extractMethodIdFromSignature(String sig) {
        int paren = sig.indexOf('(');
        return paren > 0 ? sig.substring(0, paren) : sig;
    }

    // ======================== 打印汇总 ========================

    private static void printSummary() {
        JavaParseLogUtils.logInfo("---------- Migration Summary ----------");
        JavaParseLogUtils.logInfo("Total types to migrate: " + migrationTypes.size());
        for (String type : migrationTypes) {
            String category;
            if (isModelOrEnum(type)) {
                category = classDeclarationMap.get(type) != null
                        && classDeclarationMap.get(type).isEnumDeclaration() ? "ENUM" : "MODEL";
            } else {
                Set<String> methods = usedMethods.get(type);
                category = methods != null && !methods.isEmpty()
                        ? "SERVICE(" + methods.size() + "m)"
                        : "TYPE_REF";
            }
            Path sp = classSourceFileMap.get(type);
            JavaParseLogUtils.logInfo("  [" + category + "] " + type
                    + (sp != null ? " -> " + relativizeSourcePath(sp) : " (not found)"));
        }
    }

    // ======================== 工具方法 ========================

    private static List<String> resolveEntryClassCandidates(String className) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String n = normalizeTypeName(className);
        if (n == null || n.isEmpty()) return new ArrayList<>();
        if (n.contains(".")) {
            if (allScannedClasses.contains(n)) result.add(n);
        } else {
            Set<String> cs = simpleNameToFullClass.get(n);
            if (cs != null) {
                for (String fc : cs)
                    if (methodMap.containsKey(fc + "." + JavaCodeMigratorConfig.ENTRY_METHOD_NAME)) result.add(fc);
                if (result.isEmpty()) result.addAll(cs);
            }
        }
        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    private static List<String> resolveClassCandidates(String typeName, String ctxClass) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String t = normalizeTypeName(typeName);
        if (t == null || t.isEmpty()) return new ArrayList<>();
        String simple = shortName(t);
        if (t.contains(".")) { result.add(t); return new ArrayList<>(result); }
        if (ctxClass != null) {
            Map<String, String> ei = classImportTypeMap.get(ctxClass);
            if (ei != null) {
                String imp = ei.get(simple);
                if (imp != null && !imp.isEmpty()) { result.add(imp); return new ArrayList<>(result); }
            }
            String pkg = classPackageMap.get(ctxClass);
            LinkedHashSet<String> ctxCandidates = new LinkedHashSet<>();
            if (pkg != null && !pkg.isEmpty()) ctxCandidates.add(pkg + "." + simple);
            List<String> odi = classImportOnDemandPackageMap.get(ctxClass);
            if (odi != null) for (String ip : odi) if (ip != null && !ip.isEmpty()) ctxCandidates.add(ip + "." + simple);
            for (String c : ctxCandidates) if (allScannedClasses.contains(c)) result.add(c);
            if (!result.isEmpty()) return new ArrayList<>(result);
        }
        Set<String> gcs = simpleNameToFullClass.get(simple);
        if (gcs != null) result.addAll(gcs);
        return new ArrayList<>(result);
    }

    private static MethodDeclaration resolveMethod(
            String className, String methodName, Integer argCount, List<String> argTypeHints) {
        List<MethodDeclaration> candidates = methodMap.get(className + "." + methodName);
        if (candidates == null || candidates.isEmpty()) return null;
        if (argCount == null) {
            for (MethodDeclaration c : candidates) if (c.getParameters().isEmpty()) return c;
            return candidates.get(0);
        }
        List<MethodDeclaration> exact = new ArrayList<>();
        MethodDeclaration bestVA = null;
        int bestVAPC = -1;
        for (MethodDeclaration c : candidates) {
            int pc = c.getParameters().size();
            boolean va = !c.getParameters().isEmpty() && c.getParameter(pc - 1).isVarArgs();
            if (!va && pc == argCount) { exact.add(c); }
            else if (va && argCount >= pc - 1 && pc > bestVAPC) { bestVA = c; bestVAPC = pc; }
        }
        if (!exact.isEmpty()) {
            if (exact.size() == 1) return exact.get(0);
            MethodDeclaration best = exact.get(0);
            int bestScore = Integer.MIN_VALUE;
            for (MethodDeclaration c : exact) {
                int s = scoreOverload(c, argTypeHints);
                if (s > bestScore) { best = c; bestScore = s; }
            }
            return best;
        }
        return bestVA;
    }

    private static List<String> inferCallArgTypeHints(
            MethodCallExpr call, Map<String, String> lvtm, Map<String, String> ftm) {
        List<String> h = new ArrayList<>();
        for (Expression a : call.getArguments()) h.add(inferExpressionType(a, lvtm, ftm));
        return h;
    }

    private static String inferExpressionType(
            Expression expr, Map<String, String> lvtm, Map<String, String> ftm) {
        if (expr == null) return null;
        if (expr.isNullLiteralExpr()) return "null";
        if (expr.isStringLiteralExpr()) return "String";
        if (expr.isBooleanLiteralExpr()) return "boolean";
        if (expr.isCharLiteralExpr()) return "char";
        if (expr.isIntegerLiteralExpr()) return "int";
        if (expr.isLongLiteralExpr()) return "long";
        if (expr.isDoubleLiteralExpr()) return "double";
        if (expr.isObjectCreationExpr()) return normalizeTypeName(expr.asObjectCreationExpr().getType().asString());
        if (expr.isCastExpr()) return normalizeTypeName(expr.asCastExpr().getType().asString());
        if (expr.isNameExpr()) return normalizeTypeName(lvtm.get(expr.asNameExpr().getNameAsString()));
        if (expr.isFieldAccessExpr())
            return normalizeTypeName(ftm.get(normalizeScopeVar(expr.asFieldAccessExpr().getScope().toString())));
        return null;
    }

    private static int scoreOverload(MethodDeclaration c, List<String> hints) {
        if (hints == null || hints.isEmpty()) return 0;
        int s = 0;
        int cnt = Math.min(c.getParameters().size(), hints.size());
        for (int i = 0; i < cnt; i++) {
            String pt = normalizeTypeName(c.getParameter(i).getType().asString());
            String at = normalizeTypeName(hints.get(i));
            if (at == null) continue;
            if ("null".equals(at)) { s += isPrimitiveType(pt) ? -50 : 1; continue; }
            String ps = shortName(pt), as = shortName(at);
            if (pt != null && at != null && (pt.equals(at) || ps.equals(as))) s += 8;
            else if (isPrimitiveWrapperPair(ps, as)) s += 6;
            else if ("Object".equals(ps)) s += 1;
            else s -= 4;
        }
        return s;
    }

    private static void collectTargetsByType(
            LinkedHashSet<String> targets, String typeName, String mn,
            Integer ac, List<String> ah, String ctxClass) {
        String t = normalizeTypeName(typeName);
        if (t == null) return;
        List<String> candidates = resolveClassCandidates(t, ctxClass);
        boolean mvi = false;
        for (String cand : candidates) {
            List<String> impls = interfaceToImpl.get(cand);
            if (impls != null && !impls.isEmpty()) {
                mvi = true;
                for (String impl : impls) if (resolveMethod(impl, mn, ac, ah) != null) targets.add(impl);
            }
        }
        if (!mvi) {
            List<String> impls = interfaceToImpl.get(t);
            if (impls != null) for (String impl : impls) if (resolveMethod(impl, mn, ac, ah) != null) targets.add(impl);
        }
        for (String c : candidates) if (resolveMethod(c, mn, ac, ah) != null) targets.add(c);
    }

    private static String methodSignature(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder(method.getNameAsString()).append("(");
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(cleanExpr(method.getParameter(i).getType().asString()));
        }
        return sb.append(")").toString();
    }

    private static MethodDeclaration findMethodBySignature(ClassOrInterfaceDeclaration clazz, String sig) {
        for (MethodDeclaration m : clazz.getMethods()) if (methodSignature(m).equals(sig)) return m;
        return null;
    }

    private static TypeDeclaration<?> findTypeDeclaration(CompilationUnit cu, String fullClassName, String pkg) {
        for (TypeDeclaration<?> td : cu.getTypes())
            if (fullClassName.equals(pkg.isEmpty() ? td.getNameAsString() : pkg + "." + td.getNameAsString())) return td;
        return null;
    }

    private static String relativizeSourcePath(Path path) {
        try {
            return Paths.get(JavaCodeMigratorConfig.SOURCE_PROJECT_PATH).toAbsolutePath().normalize()
                    .relativize(path.toAbsolutePath().normalize()).toString();
        } catch (Exception e) { return path.toString(); }
    }

    private static String relativeDestPath(Path destFile) {
        try {
            return Paths.get(JavaCodeMigratorConfig.DEST_PROJECT_PATH).toAbsolutePath().normalize()
                    .relativize(destFile.toAbsolutePath().normalize()).toString();
        } catch (Exception e) { return destFile.toString(); }
    }

    // ---------- 简单委托 ----------

    private static String normalizeTypeName(String s) { return JavaParseTextUtils.normalizeTypeName(s); }
    private static String shortName(String s) { return JavaParseTextUtils.shortClassName(s); }
    private static String cleanExpr(String s) { return JavaParseTextUtils.normalizeInlineWhitespace(s); }
    private static String cleanXml(String s) { return s == null ? "" : s.trim(); }
    private static String normalizeScopeVar(String s) { return JavaParseTextUtils.normalizeScopeVar(s); }
    private static boolean isTypeLikeScope(String s) { return JavaParseTextUtils.isTypeLikeScope(s); }

    private static boolean isPrimitiveType(String s) {
        if (s == null) return false;
        switch (s) {
            case "byte": case "short": case "int": case "long":
            case "float": case "double": case "boolean": case "char": return true;
            default: return false;
        }
    }

    private static boolean isPrimitiveWrapperPair(String l, String r) {
        if (l == null || r == null) return false;
        return ("byte".equals(l) && "Byte".equals(r)) || ("Byte".equals(l) && "byte".equals(r))
                || ("short".equals(l) && "Short".equals(r)) || ("Short".equals(l) && "short".equals(r))
                || ("int".equals(l) && "Integer".equals(r)) || ("Integer".equals(l) && "int".equals(r))
                || ("long".equals(l) && "Long".equals(r)) || ("Long".equals(l) && "long".equals(r))
                || ("float".equals(l) && "Float".equals(r)) || ("Float".equals(l) && "float".equals(r))
                || ("double".equals(l) && "Double".equals(r)) || ("Double".equals(l) && "double".equals(r))
                || ("boolean".equals(l) && "Boolean".equals(r)) || ("Boolean".equals(l) && "boolean".equals(r))
                || ("char".equals(l) && "Character".equals(r)) || ("Character".equals(l) && "char".equals(r));
    }

    private static boolean isJdkOrThirdPartyType(String s) {
        if (s == null) return true;
        for (String p : JavaCodeMigratorConfig.JDK_PREFIXES) if (s.startsWith(p)) return true;
        for (String p : JavaCodeMigratorConfig.THIRD_PARTY_PREFIXES) if (s.startsWith(p)) return true;
        return false;
    }

    private static void addInterfaceImpl(String iface, String impl) {
        if (iface == null || iface.isEmpty()) return;
        interfaceToImpl.computeIfAbsent(iface, k -> new ArrayList<>()).add(impl);
    }

    private static int indexedMethodCount() {
        int t = 0;
        for (List<MethodDeclaration> v : methodMap.values()) t += v.size();
        return t;
    }
}
