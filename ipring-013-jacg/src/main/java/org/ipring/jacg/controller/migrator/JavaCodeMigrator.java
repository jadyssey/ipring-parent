package org.ipring.jacg.controller.migrator;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.ipring.jacg.util.JavaMethodCallAnalysisUtils;
import org.ipring.jacg.util.JavaParseLogUtils;
import org.ipring.jacg.util.JavaParseTextUtils;
import org.ipring.jacg.util.JavaSourceScanUtils;
import org.ipring.jacg.util.ProjectMethodCompareXmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Migrates the minimum Java source set needed by a configured entry method.
 *
 * <p>The tool scans source modules, builds JavaParser based indexes, expands the
 * method call chain, copies whole value types, and writes reduced service/mapper
 * classes into the destination project. Java source generation and merge logic
 * are AST based to avoid fragile substring extraction.</p>
 */
public final class JavaCodeMigrator {

    private static final String SRC_MAIN_RESOURCES = "src/main/resources";
    private static final String XML_SUFFIX = ".xml";

    private static final List<String> SQL_STATEMENT_TAGS = Arrays.asList(
            "select", "insert", "update", "delete"
    );

    private static final List<String> XML_SUPPORT_TAGS = Arrays.asList(
            "resultMap", "sql"
    );

    private static final Set<String> MODEL_SUFFIXES = new HashSet<>(Arrays.asList(
            "DO", "DTO", "VO", "PO", "BO", "POJO", "Entity", "Model",
            "Req", "Resp", "Request", "Response", "Param", "Params",
            "Query", "Cmd", "Command", "Event", "Result", "Context"
    ));

    private static final Set<String> MODEL_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Data", "Getter", "Setter", "Builder", "Value", "NoArgsConstructor",
            "AllArgsConstructor", "ApiModel", "TableName", "Table"
    ));

    private static final Set<String> COMPONENT_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Service", "Component", "Repository", "Controller", "RestController",
            "Configuration"
    ));

    private static final Set<String> BEAN_INJECTION_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Resource", "Autowired", "Inject", "EJB", "Reference", "DubboReference",
            "DubboService", "NacosInjected"
    ));

    private final SourceIndex sourceIndex = new SourceIndex();
    private final MapperXmlIndex mapperXmlIndex = new MapperXmlIndex();
    private final MigrationPlan migrationPlan = new MigrationPlan();
    private final Set<String> destFullClassNames = new HashSet<>();

    public static void main(String[] args) throws Exception {
        configureJavaParser();
        new JavaCodeMigrator().run();
    }

    private static void configureJavaParser() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLexicalPreservationEnabled(true);
        StaticJavaParser.setConfiguration(config);
    }

    private void run() throws Exception {
        validateConfig();

        JavaParseLogUtils.logInfo("JavaCodeMigrator start.");
        JavaParseLogUtils.logInfo("  Entry class : " + JavaCodeMigratorConfig.ENTRY_CLASS_NAME);
        JavaParseLogUtils.logInfo("  Entry method: " + JavaCodeMigratorConfig.ENTRY_METHOD_NAME);
        JavaParseLogUtils.logInfo("  Source      : " + JavaCodeMigratorConfig.SOURCE_PROJECT_PATH);
        JavaParseLogUtils.logInfo("  Destination : " + JavaCodeMigratorConfig.DEST_PROJECT_PATH);

        buildDestClassNameIndex();
        scanSourceProject();
        if (!expandEntryCallChain()) {
            JavaParseLogUtils.logWarn("No entry call chain was expanded. Migration stopped.");
            return;
        }
        collectTransitiveDependencies();
        printSummary();
        migrateCode();

        JavaParseLogUtils.logInfo("JavaCodeMigrator finished. Migrated "
                + migrationPlan.migrationTypes.size() + " types.");
    }

    private void validateConfig() throws IOException {
        if (isBlank(JavaCodeMigratorConfig.ENTRY_CLASS_NAME)) {
            throw new IllegalArgumentException("ENTRY_CLASS_NAME must not be blank.");
        }
        if (isBlank(JavaCodeMigratorConfig.ENTRY_METHOD_NAME)) {
            throw new IllegalArgumentException("ENTRY_METHOD_NAME must not be blank.");
        }
        Path sourceRoot = sourceRoot();
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("Source project path not found: " + sourceRoot);
        }
        Path destRoot = destRoot();
        Files.createDirectories(destRoot);
    }

    private Path sourceRoot() {
        return Paths.get(JavaCodeMigratorConfig.SOURCE_PROJECT_PATH).toAbsolutePath().normalize();
    }

    private Path destRoot() {
        return Paths.get(JavaCodeMigratorConfig.DEST_PROJECT_PATH).toAbsolutePath().normalize();
    }

    private Path destJavaRoot() {
        return destRoot().resolve(JavaCodeMigratorConfig.SRC_MAIN_JAVA).normalize();
    }

    private Path destResourcesRoot() {
        return destRoot().resolve(SRC_MAIN_RESOURCES).normalize();
    }

    private void buildDestClassNameIndex() throws IOException {
        Path javaRoot = destJavaRoot();
        if (!Files.exists(javaRoot)) {
            JavaParseLogUtils.logInfo("Destination java dir not found, skip global index: " + javaRoot);
            return;
        }

        try (Stream<Path> stream = Files.walk(javaRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(JavaCodeMigrator::isJavaFile)
                    .forEach(path -> indexDestJavaFile(javaRoot, path));
        }
        JavaParseLogUtils.logInfo("Dest class index built: " + destFullClassNames.size() + " full names.");
    }

    private void indexDestJavaFile(Path javaRoot, Path javaFile) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile.toFile());
            String pkg = packageName(cu);
            cu.findAll(TypeDeclaration.class).forEach(type ->
                    destFullClassNames.add(fullTypeName(pkg, type)));
        } catch (Exception ex) {
            destFullClassNames.add(classNameFromPath(javaRoot, javaFile));
            JavaParseLogUtils.logWarn("Failed to parse destination java, fallback to path index: "
                    + javaFile + " (" + ex.getMessage() + ")");
        }
    }

    private String classNameFromPath(Path javaRoot, Path javaFile) {
        String relPath = normalizePath(javaRoot.relativize(javaFile).toString());
        if (!relPath.endsWith(JavaCodeMigratorConfig.JAVA_SUFFIX)) {
            return relPath.replace('/', '.');
        }
        String noSuffix = relPath.substring(0,
                relPath.length() - JavaCodeMigratorConfig.JAVA_SUFFIX.length());
        return noSuffix.replace('/', '.');
    }

    private void scanSourceProject() throws Exception {
        long start = System.currentTimeMillis();
        Path root = sourceRoot();

        List<Path> javaRoots = JavaSourceScanUtils.findAllJavaRoots(root);
        for (Path javaRoot : javaRoots) {
            scanJavaRoot(javaRoot);
        }

        sourceIndex.buildInterfaceImplementationIndex(this);

        List<Path> resourcesRoots = findMainRoots(root, "resources");
        for (Path resourcesRoot : resourcesRoots) {
            scanMapperXmlRoot(resourcesRoot);
        }

        long elapsed = System.currentTimeMillis() - start;
        JavaParseLogUtils.logInfo("Scan completed: " + sourceIndex.allClasses.size()
                + " classes, " + sourceIndex.indexedMethodCount() + " methods, "
                + mapperXmlIndex.statementSqlByKey.size() + " mapper SQLs, elapsed="
                + elapsed + "ms");
    }

    private void scanJavaRoot(Path javaRoot) throws Exception {
        for (Path javaFile : JavaSourceScanUtils.collectJavaFiles(javaRoot)) {
            processJavaFile(javaFile);
        }
    }

    private void processJavaFile(Path javaFile) throws IOException {
        String rawText = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(rawText);
        } catch (Exception ex) {
            JavaParseLogUtils.logWarn("Failed to parse source java: " + javaFile
                    + " (" + ex.getMessage() + ")");
            return;
        }

        String pkg = packageName(cu);
        JavaMethodCallAnalysisUtils.ImportContext importContext =
                JavaMethodCallAnalysisUtils.buildImportContext(cu.getImports());
        Path normalizedPath = javaFile.toAbsolutePath().normalize();

        for (TypeDeclaration<?> typeDecl : cu.getTypes()) {
            String fullName = toFullClassName(pkg, typeDecl.getNameAsString());
            sourceIndex.indexTopLevelType(fullName, pkg, typeDecl, cu, rawText,
                    normalizedPath, importContext);
        }
    }

    private List<Path> findMainRoots(Path root, String leafName) throws IOException {
        List<Path> roots = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> leafName.equalsIgnoreCase(path.getFileName().toString()))
                    .filter(this::isSrcMainLeaf)
                    .filter(path -> !isBuildOutputPath(path))
                    .forEach(path -> roots.add(path.toAbsolutePath().normalize()));
        }
        roots.sort(Comparator.comparing(Path::toString));
        return roots;
    }

    private boolean isSrcMainLeaf(Path path) {
        Path parent = path.getParent();
        if (parent == null || parent.getFileName() == null) {
            return false;
        }
        Path grand = parent.getParent();
        if (grand == null || grand.getFileName() == null) {
            return false;
        }
        return "main".equalsIgnoreCase(parent.getFileName().toString())
                && "src".equalsIgnoreCase(grand.getFileName().toString());
    }

    private boolean isBuildOutputPath(Path path) {
        String normalized = normalizePath(path.toString());
        return normalized.contains("/target/") || normalized.contains("/build/");
    }

    private void scanMapperXmlRoot(Path resourcesRoot) {
        try (Stream<Path> stream = Files.walk(resourcesRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(XML_SUFFIX))
                    .forEach(path -> parseMapperXmlFile(resourcesRoot, path));
        } catch (IOException ex) {
            JavaParseLogUtils.logWarn("Failed to scan mapper xml root: " + resourcesRoot
                    + " (" + ex.getMessage() + ")");
        }
    }

    private void parseMapperXmlFile(Path resourcesRoot, Path xmlFile) {
        try {
            String rawXml = new String(Files.readAllBytes(xmlFile), StandardCharsets.UTF_8);
            Document doc = parseXml(rawXml);
            Element root = doc.getDocumentElement();
            if (root == null || !"mapper".equals(root.getTagName())) {
                return;
            }

            String namespace = trim(root.getAttribute("namespace"));
            if (namespace.isEmpty()) {
                return;
            }

            String relPath = normalizePath(resourcesRoot.toAbsolutePath().normalize()
                    .relativize(xmlFile.toAbsolutePath().normalize()).toString());
            mapperXmlIndex.indexMapper(namespace, xmlFile.toAbsolutePath().normalize(), relPath, rawXml);

            indexXmlSupportElements(namespace, root);
            indexXmlStatementElements(namespace, root);
        } catch (Exception ex) {
            JavaParseLogUtils.logWarn("Skip non-mybatis or invalid xml: " + xmlFile
                    + " (" + ex.getMessage() + ")");
        }
    }

    private void indexXmlSupportElements(String namespace, Element mapperRoot) throws Exception {
        for (String tag : XML_SUPPORT_TAGS) {
            org.w3c.dom.NodeList nodes = mapperRoot.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                String id = trim(element.getAttribute("id"));
                if (id.isEmpty()) {
                    continue;
                }
                String key = namespace + "." + id;
                mapperXmlIndex.supportSqlByKey.put(key, serializeXmlElement(element));
                mapperXmlIndex.dependenciesByKey.put(key, collectXmlDependencies(namespace, element));
            }
        }
    }

    private void indexXmlStatementElements(String namespace, Element mapperRoot) throws Exception {
        for (String tag : SQL_STATEMENT_TAGS) {
            org.w3c.dom.NodeList nodes = mapperRoot.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                String id = trim(element.getAttribute("id"));
                if (id.isEmpty()) {
                    continue;
                }
                String key = namespace + "." + id;
                mapperXmlIndex.statementSqlByKey.put(key, serializeXmlElement(element));
                mapperXmlIndex.dependenciesByKey.put(key, collectXmlDependencies(namespace, element));
            }
        }
    }

    private Set<String> collectXmlDependencies(String namespace, Element element) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        collectXmlDependencyAttributes(namespace, element, dependencies);
        org.w3c.dom.NodeList descendants = element.getElementsByTagName("*");
        for (int i = 0; i < descendants.getLength(); i++) {
            org.w3c.dom.Node node = descendants.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                collectXmlDependencyAttributes(namespace, (Element) node, dependencies);
            }
        }
        return dependencies;
    }

    private void collectXmlDependencyAttributes(String namespace, Element element, Set<String> dependencies) {
        String resultMap = trim(element.getAttribute("resultMap"));
        if (!resultMap.isEmpty()) {
            dependencies.add(resolveXmlRef(namespace, resultMap));
        }
        String refId = trim(element.getAttribute("refid"));
        if (!refId.isEmpty()) {
            dependencies.add(resolveXmlRef(namespace, refId));
        }
    }

    private String resolveXmlRef(String namespace, String refId) {
        String value = trim(refId);
        if (value.isEmpty()) {
            return value;
        }
        return value.contains(".") ? value : namespace + "." + value;
    }

    private Document parseXml(String rawXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory,
                "http://apache.org/xml/features/disallow-doctype-decl", false);
        ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory,
                "http://xml.org/sax/features/external-general-entities", false);
        ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory,
                "http://xml.org/sax/features/external-parameter-entities", false);
        ProjectMethodCompareXmlUtils.setSafeXmlFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void fatalError(SAXParseException e) {
                // The caller treats parse failures as non-mybatis or invalid XML.
            }
        });
        return builder.parse(new InputSource(new StringReader(rawXml)));
    }

    private String serializeXmlElement(Element element) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ignored) {
            // Not all XML transformer implementations support this feature.
        }
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }

    private boolean expandEntryCallChain() {
        List<String> candidates = resolveEntryClassCandidates(JavaCodeMigratorConfig.ENTRY_CLASS_NAME);
        if (candidates.isEmpty()) {
            JavaParseLogUtils.logWarn("Entry class not found in source project: "
                    + JavaCodeMigratorConfig.ENTRY_CLASS_NAME);
            return false;
        }
        if (candidates.size() > 1) {
            JavaParseLogUtils.logInfo("Multiple entry class candidates found: " + candidates);
        }

        for (String fullClassName : candidates) {
            addMigrationType(fullClassName);
            expandMethodCallChain(fullClassName, JavaCodeMigratorConfig.ENTRY_METHOD_NAME,
                    null, null, 0);
        }
        return true;
    }

    private void expandMethodCallChain(
            String className,
            String methodName,
            Integer argCount,
            List<String> argTypeHints,
            int level
    ) {
        if (level > JavaCodeMigratorConfig.MAX_EXPAND_LEVEL) {
            JavaParseLogUtils.logWarn("Max expand level reached at " + className + "." + methodName);
            return;
        }

        MethodDeclaration method = resolveMethod(className, methodName, argCount, argTypeHints);
        if (method == null) {
            return;
        }

        String identityKey = className + "#" + methodSignature(method);
        if (!migrationPlan.expandedMethodKeys.add(identityKey)) {
            return;
        }

        migrationPlan.recordUsedMethod(className, method);
        collectMethodSignatureTypes(method, className);

        if (!method.getBody().isPresent()) {
            expandImplementations(className, methodName, argCount, argTypeHints, level);
            return;
        }

        Map<String, String> localVarTypeMap = buildLocalVarTypeMap(method);
        Map<String, String> fieldMap = sourceIndex.fieldTypesByClass.getOrDefault(
                className, Collections.emptyMap());

        Node body = method.getBody().get();
        collectTypeReferencesFromNode(body, className);
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            expandMethodCall(call, className, localVarTypeMap, fieldMap, level);
        }
    }

    private Map<String, String> buildLocalVarTypeMap(MethodDeclaration method) {
        Map<String, String> map = JavaMethodCallAnalysisUtils.buildLocalVarTypeMap(method);
        method.findAll(CatchClause.class).forEach(catchClause ->
                map.put(catchClause.getParameter().getNameAsString(),
                        normalizeTypeName(catchClause.getParameter().getType().asString())));
        return map;
    }

    private void expandImplementations(
            String className,
            String methodName,
            Integer argCount,
            List<String> argTypeHints,
            int level
    ) {
        LinkedHashSet<String> impls = new LinkedHashSet<>();
        addAll(impls, sourceIndex.interfaceToImplementations.get(className));
        addAll(impls, sourceIndex.interfaceToImplementations.get(shortName(className)));

        for (String impl : impls) {
            if (resolveMethod(impl, methodName, argCount, argTypeHints) == null) {
                continue;
            }
            addMigrationType(impl);
            expandMethodCallChain(impl, methodName, argCount, argTypeHints, level + 1);
        }
    }

    private void expandMethodCall(
            MethodCallExpr call,
            String contextClass,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap,
            int level
    ) {
        String calledMethodName = call.getNameAsString();
        int callArgCount = call.getArguments().size();
        List<String> callArgHints = inferCallArgTypeHints(call, localVarTypeMap, fieldMap);
        LinkedHashSet<String> targets = resolveCallTargets(call, contextClass,
                localVarTypeMap, fieldMap, callArgCount, callArgHints);

        for (String targetClass : targets) {
            addMigrationType(targetClass);
            expandMethodCallChain(targetClass, calledMethodName, callArgCount,
                    callArgHints, level + 1);
        }
    }

    private LinkedHashSet<String> resolveCallTargets(
            MethodCallExpr call,
            String contextClass,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap,
            Integer argCount,
            List<String> argTypeHints
    ) {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        String methodName = call.getNameAsString();

        if (!call.getScope().isPresent()) {
            if (resolveMethod(contextClass, methodName, argCount, argTypeHints) != null) {
                targets.add(contextClass);
            }
            return targets;
        }

        Expression scope = call.getScope().get();
        String scopeExpr = cleanExpr(scope.toString());
        String scopeVar = normalizeScopeVar(scopeExpr);

        if ("this".equals(scopeVar) || "super".equals(scopeVar)) {
            if (resolveMethod(contextClass, methodName, argCount, argTypeHints) != null) {
                targets.add(contextClass);
            }
            return targets;
        }

        String scopeType = JavaMethodCallAnalysisUtils.inferScopeType(scopeExpr,
                localVarTypeMap, fieldMap);
        if (scopeType != null) {
            collectTargetsByType(targets, scopeType, methodName, argCount, argTypeHints,
                    contextClass);
            return targets;
        }

        if (isTypeLikeScope(scopeExpr)) {
            collectTargetsByType(targets, scopeExpr, methodName, argCount, argTypeHints,
                    contextClass);
        }
        return targets;
    }

    private void collectMethodSignatureTypes(MethodDeclaration method, String contextClassName) {
        collectTypeReference(method.getType(), contextClassName);
        for (Parameter param : method.getParameters()) {
            collectTypeReference(param.getType(), contextClassName);
            param.getAnnotations().forEach(ann -> collectTypeName(ann.getNameAsString(), contextClassName));
        }
        method.getThrownExceptions().forEach(type -> collectTypeReference(type, contextClassName));
        method.getAnnotations().forEach(ann -> collectTypeName(ann.getNameAsString(), contextClassName));
    }

    private void collectTypeReferencesFromNode(Node node, String contextClassName) {
        node.findAll(ClassOrInterfaceType.class).forEach(type ->
                collectTypeReference(type, contextClassName));
        node.findAll(ObjectCreationExpr.class).forEach(expr ->
                collectTypeReference(expr.getType(), contextClassName));
        node.findAll(CastExpr.class).forEach(expr ->
                collectTypeReference(expr.getType(), contextClassName));
        node.findAll(ClassExpr.class).forEach(expr ->
                collectTypeReference(expr.getType(), contextClassName));
        node.findAll(AnnotationExpr.class).forEach(ann ->
                collectTypeName(ann.getNameAsString(), contextClassName));
        node.findAll(FieldAccessExpr.class).forEach(expr -> {
            String scopeStr = expr.getScope().toString();
            if (isTypeLikeScope(scopeStr)) {
                collectTypeName(scopeStr, contextClassName);
            }
        });
    }

    private void collectTypeReference(Type type, String contextClassName) {
        if (type == null) {
            return;
        }
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType classType = type.asClassOrInterfaceType();
            collectTypeName(classType.getNameAsString(), contextClassName);
            classType.getTypeArguments().ifPresent(args ->
                    args.forEach(arg -> collectTypeReference(arg, contextClassName)));
            classType.getScope().ifPresent(scope -> collectTypeReference(scope, contextClassName));
            return;
        }
        if (type.isArrayType()) {
            collectTypeReference(type.asArrayType().getComponentType(), contextClassName);
            return;
        }
        if (type.isWildcardType()) {
            type.asWildcardType().getExtendedType().ifPresent(t -> collectTypeReference(t, contextClassName));
            type.asWildcardType().getSuperType().ifPresent(t -> collectTypeReference(t, contextClassName));
            return;
        }
        if (type.isUnionType()) {
            type.asUnionType().getElements().forEach(t -> collectTypeReference(t, contextClassName));
            return;
        }
        if (type.isIntersectionType()) {
            type.asIntersectionType().getElements().forEach(t -> collectTypeReference(t, contextClassName));
        }
    }

    private void collectTypeName(String typeName, String contextClassName) {
        String normalized = normalizeTypeName(typeName);
        if (normalized == null || normalized.isEmpty()
                || isPrimitiveType(normalized)
                || isJdkOrThirdPartyType(normalized)) {
            return;
        }

        if (normalized.contains(".")) {
            addMigrationType(normalized);
            if (contextClassName != null) {
                for (String candidate : resolveClassCandidates(normalized, contextClassName)) {
                    addMigrationType(candidate);
                }
            }
            return;
        }

        for (String candidate : resolveClassCandidates(normalized, contextClassName)) {
            if (!isJdkOrThirdPartyType(candidate)) {
                addMigrationType(candidate);
            }
        }
    }

    private void analyzeClassTypeReferences(String fullClassName) {
        if (!migrationPlan.analyzedTypes.add(fullClassName)) {
            return;
        }
        TypeDeclaration<?> declaration = sourceIndex.declarationByClass.get(fullClassName);
        if (declaration == null) {
            return;
        }

        if (shouldCopyWholeType(fullClassName)) {
            collectTypeReferencesFromNode(declaration, fullClassName);
            return;
        }

        declaration.getAnnotations().forEach(ann ->
                collectTypeName(ann.getNameAsString(), fullClassName));

        if (declaration instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration;
            clazz.getExtendedTypes().forEach(type -> collectTypeReference(type, fullClassName));
            clazz.getImplementedTypes().forEach(type -> collectTypeReference(type, fullClassName));
            clazz.getTypeParameters().forEach(typeParameter ->
                    typeParameter.getTypeBound().forEach(bound ->
                            collectTypeReference(bound, fullClassName)));
            clazz.getFields().forEach(field -> collectTypeReferencesFromNode(field, fullClassName));
            clazz.getConstructors().forEach(constructor ->
                    collectTypeReferencesFromNode(constructor, fullClassName));
            clazz.getMembers().forEach(member -> {
                if (member instanceof InitializerDeclaration || member instanceof TypeDeclaration) {
                    collectTypeReferencesFromNode(member, fullClassName);
                }
            });
        } else if (declaration instanceof EnumDeclaration) {
            collectTypeReferencesFromNode(declaration, fullClassName);
        } else if (declaration instanceof AnnotationDeclaration) {
            collectTypeReferencesFromNode(declaration, fullClassName);
        }
    }

    private void addMigrationType(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return;
        }
        if (isJdkOrThirdPartyType(fullClassName)) {
            return;
        }
        if (!sourceIndex.allClasses.contains(fullClassName)
                && !sourceIndex.sourceFileByClass.containsKey(fullClassName)) {
            return;
        }
        if (migrationPlan.migrationTypes.add(fullClassName)) {
            analyzeClassTypeReferences(fullClassName);
        }
    }

    private void collectTransitiveDependencies() {
        int previousSize;
        do {
            previousSize = migrationPlan.migrationTypes.size();
            List<String> snapshot = new ArrayList<>(migrationPlan.migrationTypes);
            for (String type : snapshot) {
                analyzeClassTypeReferences(type);
            }
        } while (migrationPlan.migrationTypes.size() > previousSize);
    }

    private void migrateCode() throws IOException {
        MigrationStats stats = new MigrationStats();

        for (String fullClassName : migrationPlan.migrationTypes) {
            try {
                migrateOneType(fullClassName, stats);
            } catch (Exception ex) {
                stats.failed++;
                JavaParseLogUtils.logWarn("Failed to migrate " + fullClassName + ": "
                        + ex.getMessage());
            }
        }

        JavaParseLogUtils.logInfo("Migration completed: "
                + "wholeCopied=" + stats.wholeCopied
                + ", wholeSkipped=" + stats.wholeSkipped
                + ", methodNewFile=" + stats.methodNewFile
                + ", methodMerged=" + stats.methodMerged
                + ", callChainSkipped=" + stats.callChainSkipped
                + ", xmlSynced=" + stats.xmlSynced
                + ", failed=" + stats.failed);
    }

    private void migrateOneType(String fullClassName, MigrationStats stats) throws IOException {
        Path srcFile = sourceIndex.sourceFileByClass.get(fullClassName);
        if (srcFile == null) {
            stats.failed++;
            JavaParseLogUtils.logWarn("Source file not found for " + fullClassName);
            return;
        }

        String pkg = sourceIndex.packageByClass.get(fullClassName);
        if (pkg == null) {
            stats.failed++;
            JavaParseLogUtils.logWarn("Package not found for " + fullClassName);
            return;
        }

        boolean mapper = isMapperClass(fullClassName);
        Set<String> usedMethods = migrationPlan.usedMethods.get(fullClassName);
        Path destFile = destinationJavaFile(pkg, shortName(fullClassName));

        if (shouldCopyWholeType(fullClassName) && !(mapper && hasUsedMethods(usedMethods))) {
            copyWholeType(fullClassName, srcFile, destFile, stats);
            return;
        }

        if (isInterface(fullClassName) && !mapper) {
            copyWholeType(fullClassName, srcFile, destFile, stats);
            return;
        }

        if (!hasUsedMethods(usedMethods)) {
            stats.callChainSkipped++;
            JavaParseLogUtils.logInfo("SKIP (not in call chain): " + fullClassName);
            return;
        }

        Files.createDirectories(destFile.getParent());
        if (Files.exists(destFile)) {
            mergeMembersToExisting(fullClassName, usedMethods, destFile);
            stats.methodMerged++;
        } else {
            writeReducedClass(fullClassName, usedMethods, destFile);
            stats.methodNewFile++;
        }

        destFullClassNames.add(fullClassName);
        if (mapper) {
            stats.xmlSynced += syncMapperXml(fullClassName, usedMethods);
        }
    }

    private Path destinationJavaFile(String pkg, String simpleName) {
        Path packageDir = pkg.isEmpty()
                ? destJavaRoot()
                : destJavaRoot().resolve(pkg.replace('.', '/'));
        return packageDir.resolve(simpleName + JavaCodeMigratorConfig.JAVA_SUFFIX);
    }

    private void copyWholeType(
            String fullClassName,
            Path srcFile,
            Path destFile,
            MigrationStats stats
    ) throws IOException {
        if (destFullClassNames.contains(fullClassName) || Files.exists(destFile)) {
            stats.wholeSkipped++;
            JavaParseLogUtils.logInfo("SKIP (whole type exists): " + fullClassName);
            return;
        }
        Files.createDirectories(destFile.getParent());
        Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
        destFullClassNames.add(fullClassName);
        stats.wholeCopied++;
        JavaParseLogUtils.logInfo("COPY (whole type): " + relativeDestPath(destFile));
    }

    private void writeReducedClass(
            String fullClassName,
            Set<String> methodSignatures,
            Path destFile
    ) throws IOException {
        CompilationUnit sourceCu = sourceIndex.compilationUnitByClass.get(fullClassName);
        if (sourceCu == null) {
            throw new IOException("Source CompilationUnit not found: " + fullClassName);
        }

        CompilationUnit outputCu = sourceCu.clone();
        setupLexicalPreservation(outputCu);
        keepOnlyTargetTopLevelType(outputCu, fullClassName);

        TypeDeclaration<?> targetType = findTypeDeclaration(outputCu, fullClassName);
        if (!(targetType instanceof ClassOrInterfaceDeclaration)) {
            String text = printCompilationUnit(outputCu);
            writeValidJavaOrFallback(fullClassName, destFile, text);
            JavaParseLogUtils.logInfo("NEW (whole non-class type): " + relativeDestPath(destFile));
            return;
        }

        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) targetType;
        Set<String> effectiveMethodSignatures = expandLocalSupportMethodSignatures(fullClassName, methodSignatures);
        Set<String> referencedFields = collectReferencedFieldNames(clazz, effectiveMethodSignatures);
        int prunedBeanFields = pruneUnusedBeanInjectionFields(clazz, referencedFields);
        pruneUnusedMethods(clazz, effectiveMethodSignatures);

        String text = printCompilationUnit(outputCu);
        writeValidJavaOrFallback(fullClassName, destFile, text);
        JavaParseLogUtils.logInfo("NEW (reduced, " + effectiveMethodSignatures.size()
                + " methods, prunedBeans=" + prunedBeanFields + "): "
                + relativeDestPath(destFile));
    }

    private Set<String> expandLocalSupportMethodSignatures(
            String fullClassName,
            Set<String> baseSignatures
    ) {
        TypeDeclaration<?> type = sourceIndex.declarationByClass.get(fullClassName);
        if (!(type instanceof ClassOrInterfaceDeclaration)) {
            return baseSignatures;
        }

        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) type;
        LinkedHashSet<String> result = new LinkedHashSet<>(baseSignatures);
        boolean changed;
        do {
            changed = false;
            for (Node node : localSupportScanNodes(clazz, result)) {
                for (MethodCallExpr call : node.findAll(MethodCallExpr.class)) {
                    if (!isSelfCall(call)) {
                        continue;
                    }
                    MethodDeclaration target = resolveMethod(fullClassName, call.getNameAsString(),
                            call.getArguments().size(), Collections.emptyList());
                    if (target != null && result.add(methodSignature(target))) {
                        changed = true;
                    }
                }
            }
        } while (changed);
        return result;
    }

    private List<Node> localSupportScanNodes(
            ClassOrInterfaceDeclaration clazz,
            Set<String> retainedMethodSignatures
    ) {
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(clazz.getConstructors());
        clazz.getMembers().forEach(member -> {
            if (member instanceof FieldDeclaration || member instanceof InitializerDeclaration) {
                nodes.add(member);
            }
        });
        for (MethodDeclaration method : clazz.getMethods()) {
            if (retainedMethodSignatures.contains(methodSignature(method))) {
                nodes.add(method);
            }
        }
        return nodes;
    }

    private boolean isSelfCall(MethodCallExpr call) {
        if (!call.getScope().isPresent()) {
            return true;
        }
        String scope = normalizeScopeVar(cleanExpr(call.getScope().get().toString()));
        return "this".equals(scope) || "super".equals(scope);
    }

    private Set<String> collectReferencedFieldNames(
            ClassOrInterfaceDeclaration clazz,
            Set<String> retainedMethodSignatures
    ) {
        Set<String> classFieldNames = fieldNames(clazz);
        Set<String> referencedFields = new LinkedHashSet<>();
        if (classFieldNames.isEmpty()) {
            return referencedFields;
        }

        clazz.getAnnotations().forEach(annotation ->
                collectFieldReferences(annotation, classFieldNames, referencedFields));
        clazz.getFields().forEach(field ->
                collectFieldReferences(field, classFieldNames, referencedFields));
        clazz.getConstructors().forEach(constructor ->
                collectFieldReferences(constructor, classFieldNames, referencedFields));
        clazz.getMembers().forEach(member -> {
            if (member instanceof InitializerDeclaration) {
                collectFieldReferences(member, classFieldNames, referencedFields);
            }
        });
        for (MethodDeclaration method : clazz.getMethods()) {
            if (retainedMethodSignatures.contains(methodSignature(method))) {
                collectFieldReferences(method, classFieldNames, referencedFields);
            }
        }
        return referencedFields;
    }

    @SuppressWarnings("unchecked")
    private void collectFieldReferences(
            Node node,
            Set<String> classFieldNames,
            Set<String> referencedFields
    ) {
        node.findAll(FieldAccessExpr.class).forEach(fieldAccess -> {
            String fieldName = fieldAccess.getNameAsString();
            if (!classFieldNames.contains(fieldName)) {
                return;
            }
            String scope = normalizeScopeVar(cleanExpr(fieldAccess.getScope().toString()));
            if ("this".equals(scope) || "super".equals(scope) || scope.endsWith(".this")) {
                referencedFields.add(fieldName);
            }
        });

        node.findAll(NameExpr.class).forEach(nameExpr -> {
            String name = nameExpr.getNameAsString();
            if (classFieldNames.contains(name) && !isShadowedByLocalName(nameExpr, name)) {
                referencedFields.add(name);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private boolean isShadowedByLocalName(NameExpr nameExpr, String name) {
        Optional<MethodDeclaration> method = nameExpr.findAncestor(MethodDeclaration.class);
        if (method.isPresent()
                && method.get().getParameters().stream().anyMatch(p -> name.equals(p.getNameAsString()))) {
            return true;
        }

        Optional<ConstructorDeclaration> constructor = nameExpr.findAncestor(ConstructorDeclaration.class);
        if (constructor.isPresent()
                && constructor.get().getParameters().stream().anyMatch(p -> name.equals(p.getNameAsString()))) {
            return true;
        }

        Optional<CatchClause> catchClause = nameExpr.findAncestor(CatchClause.class);
        if (catchClause.isPresent()
                && name.equals(catchClause.get().getParameter().getNameAsString())) {
            return true;
        }

        Node searchRoot = method.<Node>map(m -> m)
                .orElseGet(() -> constructor.<Node>map(c -> c)
                        .orElseGet(() -> catchClause.<Node>map(c -> c).orElse(null)));
        if (searchRoot == null) {
            searchRoot = nameExpr.findRootNode();
        }

        for (VariableDeclarator variable : searchRoot.findAll(VariableDeclarator.class)) {
            if (!name.equals(variable.getNameAsString())
                    || variable.findAncestor(FieldDeclaration.class).isPresent()
                    || !declaredBefore(variable, nameExpr)
                    || !isPotentiallyInScope(variable, nameExpr)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean declaredBefore(Node declarationNode, Node usageNode) {
        if (!declarationNode.getRange().isPresent() || !usageNode.getRange().isPresent()) {
            return true;
        }
        com.github.javaparser.Position declaration = declarationNode.getRange().get().begin;
        com.github.javaparser.Position usage = usageNode.getRange().get().begin;
        return declaration.line < usage.line
                || declaration.line == usage.line && declaration.column < usage.column;
    }

    @SuppressWarnings("unchecked")
    private boolean isPotentiallyInScope(VariableDeclarator variable, NameExpr nameExpr) {
        Optional<com.github.javaparser.ast.stmt.ForStmt> forStmt =
                variable.findAncestor(com.github.javaparser.ast.stmt.ForStmt.class);
        if (forStmt.isPresent()) {
            return isAncestor(forStmt.get(), nameExpr);
        }

        Optional<com.github.javaparser.ast.stmt.ForEachStmt> forEachStmt =
                variable.findAncestor(com.github.javaparser.ast.stmt.ForEachStmt.class);
        if (forEachStmt.isPresent()) {
            return isAncestor(forEachStmt.get(), nameExpr);
        }

        Optional<com.github.javaparser.ast.stmt.BlockStmt> variableBlock =
                variable.findAncestor(com.github.javaparser.ast.stmt.BlockStmt.class);
        Optional<com.github.javaparser.ast.stmt.BlockStmt> usageBlock =
                nameExpr.findAncestor(com.github.javaparser.ast.stmt.BlockStmt.class);
        if (variableBlock.isPresent() && usageBlock.isPresent()) {
            return variableBlock.get() == usageBlock.get()
                    || isAncestor(variableBlock.get(), usageBlock.get());
        }
        return true;
    }

    private boolean isAncestor(Node ancestor, Node node) {
        Optional<Node> parent = node.getParentNode();
        while (parent.isPresent()) {
            if (parent.get() == ancestor) {
                return true;
            }
            parent = parent.get().getParentNode();
        }
        return false;
    }

    private int pruneUnusedBeanInjectionFields(
            ClassOrInterfaceDeclaration clazz,
            Set<String> referencedFields
    ) {
        int removed = 0;
        List<FieldDeclaration> emptyFields = new ArrayList<>();
        for (FieldDeclaration field : clazz.getFields()) {
            if (!isBeanInjectionField(field)) {
                continue;
            }
            List<VariableDeclarator> unusedVariables = new ArrayList<>();
            for (VariableDeclarator variable : field.getVariables()) {
                if (!referencedFields.contains(variable.getNameAsString())) {
                    unusedVariables.add(variable);
                }
            }
            for (VariableDeclarator variable : unusedVariables) {
                field.getVariables().remove(variable);
                removed++;
            }
            if (field.getVariables().isEmpty()) {
                emptyFields.add(field);
            }
        }
        emptyFields.forEach(Node::remove);
        return removed;
    }

    private boolean isBeanInjectionField(FieldDeclaration field) {
        for (AnnotationExpr annotation : field.getAnnotations()) {
            String simpleName = shortName(annotation.getNameAsString());
            if (BEAN_INJECTION_ANNOTATIONS.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private void keepOnlyTargetTopLevelType(CompilationUnit cu, String fullClassName) {
        String pkg = packageName(cu);
        List<TypeDeclaration<?>> removeList = new ArrayList<>();
        for (TypeDeclaration<?> type : cu.getTypes()) {
            String currentFullName = toFullClassName(pkg, type.getNameAsString());
            if (!fullClassName.equals(currentFullName)) {
                removeList.add(type);
            }
        }
        removeList.forEach(Node::remove);
    }

    private void pruneUnusedMethods(ClassOrInterfaceDeclaration clazz, Set<String> methodSignatures) {
        List<MethodDeclaration> removeList = new ArrayList<>();
        for (MethodDeclaration method : clazz.getMethods()) {
            if (!methodSignatures.contains(methodSignature(method))) {
                removeList.add(method);
            }
        }
        removeList.forEach(Node::remove);
    }

    private void mergeMembersToExisting(
            String fullClassName,
            Set<String> methodSignatures,
            Path destFile
    ) throws IOException {
        CompilationUnit destCu;
        try {
            destCu = StaticJavaParser.parse(destFile.toFile());
        } catch (Exception ex) {
            JavaParseLogUtils.logWarn("Failed to parse dest file, creating reduced file: "
                    + destFile + " (" + ex.getMessage() + ")");
            writeReducedClass(fullClassName, methodSignatures, destFile);
            return;
        }

        CompilationUnit sourceCu = sourceIndex.compilationUnitByClass.get(fullClassName);
        TypeDeclaration<?> sourceType = findTypeDeclaration(sourceCu, fullClassName);
        TypeDeclaration<?> destType = findTypeDeclaration(destCu, fullClassName);
        if (!(sourceType instanceof ClassOrInterfaceDeclaration)
                || !(destType instanceof ClassOrInterfaceDeclaration)) {
            JavaParseLogUtils.logWarn("Type mismatch, skip merge: " + fullClassName);
            return;
        }

        setupLexicalPreservation(destCu);
        ClassOrInterfaceDeclaration sourceClass = (ClassOrInterfaceDeclaration) sourceType;
        ClassOrInterfaceDeclaration destClass = (ClassOrInterfaceDeclaration) destType;

        Set<String> effectiveMethodSignatures = expandLocalSupportMethodSignatures(fullClassName, methodSignatures);
        Set<String> referencedFields = collectReferencedFieldNames(sourceClass, effectiveMethodSignatures);

        int importsAdded = mergeImports(sourceCu, destCu);
        int fieldsAdded = mergeMissingFields(sourceClass, destClass, referencedFields);
        int constructorsAdded = mergeMissingConstructors(sourceClass, destClass);
        MergeMethodResult methodResult = mergeMethods(sourceClass, destClass, effectiveMethodSignatures);

        String output = printCompilationUnit(destCu);
        if (!isValidJavaSource(output, "MERGE " + fullClassName)) {
            JavaParseLogUtils.logWarn("Generated merge result is invalid, merge aborted: " + fullClassName);
            return;
        }

        Files.write(destFile, output.getBytes(StandardCharsets.UTF_8));
        JavaParseLogUtils.logInfo("MERGE (imports=" + importsAdded
                + ", fields=" + fieldsAdded
                + ", constructors=" + constructorsAdded
                + ", added=" + methodResult.added
                + ", overwritten=" + methodResult.overwritten
                + "): " + relativeDestPath(destFile));
    }

    private int mergeImports(CompilationUnit sourceCu, CompilationUnit destCu) {
        Set<String> existingKeys = new HashSet<>();
        Map<String, String> existingExplicitSimpleNames = new HashMap<>();

        destCu.getImports().forEach(imp -> {
            existingKeys.add(importKey(imp));
            if (!imp.isStatic() && !imp.isAsterisk()) {
                existingExplicitSimpleNames.put(shortName(imp.getNameAsString()), imp.getNameAsString());
            }
        });

        int added = 0;
        for (com.github.javaparser.ast.ImportDeclaration sourceImport : sourceCu.getImports()) {
            String key = importKey(sourceImport);
            if (existingKeys.contains(key)) {
                continue;
            }
            if (!sourceImport.isStatic() && !sourceImport.isAsterisk()) {
                String simpleName = shortName(sourceImport.getNameAsString());
                String existingFullName = existingExplicitSimpleNames.get(simpleName);
                if (existingFullName != null
                        && !existingFullName.equals(sourceImport.getNameAsString())) {
                    JavaParseLogUtils.logWarn("IMPORT CONFLICT, skip " + sourceImport
                            + " because " + existingFullName + " already exists.");
                    continue;
                }
                existingExplicitSimpleNames.put(simpleName, sourceImport.getNameAsString());
            }
            destCu.getImports().add(sourceImport.clone());
            existingKeys.add(key);
            added++;
        }
        return added;
    }

    private String importKey(com.github.javaparser.ast.ImportDeclaration imp) {
        return imp.isStatic() + "|" + imp.isAsterisk() + "|" + imp.getNameAsString();
    }

    private int mergeMissingFields(
            ClassOrInterfaceDeclaration sourceClass,
            ClassOrInterfaceDeclaration destClass,
            Set<String> referencedFields
    ) {
        Set<String> destFieldNames = fieldNames(destClass);
        int added = 0;
        for (FieldDeclaration sourceField : sourceClass.getFields()) {
            boolean beanInjectionField = isBeanInjectionField(sourceField);
            FieldDeclaration copy = sourceField.clone();
            copy.getVariables().clear();
            for (VariableDeclarator variable : sourceField.getVariables()) {
                String name = variable.getNameAsString();
                if (destFieldNames.contains(name)
                        || beanInjectionField && !referencedFields.contains(name)) {
                    continue;
                }
                copy.getVariables().add(variable.clone());
                destFieldNames.add(name);
            }
            if (!copy.getVariables().isEmpty()) {
                insertMember(destClass, copy, MemberInsertPosition.FIELD);
                added++;
            }
        }
        return added;
    }

    private Set<String> fieldNames(ClassOrInterfaceDeclaration clazz) {
        Set<String> names = new HashSet<>();
        clazz.getFields().forEach(field ->
                field.getVariables().forEach(var -> names.add(var.getNameAsString())));
        return names;
    }

    private int mergeMissingConstructors(
            ClassOrInterfaceDeclaration sourceClass,
            ClassOrInterfaceDeclaration destClass
    ) {
        if (destClass.isInterface()) {
            return 0;
        }

        Set<String> existing = new HashSet<>();
        destClass.getConstructors().forEach(constructor ->
                existing.add(constructorSignature(constructor)));

        int added = 0;
        for (ConstructorDeclaration constructor : sourceClass.getConstructors()) {
            String signature = constructorSignature(constructor);
            if (existing.contains(signature)) {
                continue;
            }
            insertMember(destClass, constructor.clone(), MemberInsertPosition.CONSTRUCTOR);
            existing.add(signature);
            added++;
        }
        return added;
    }

    private MergeMethodResult mergeMethods(
            ClassOrInterfaceDeclaration sourceClass,
            ClassOrInterfaceDeclaration destClass,
            Set<String> methodSignatures
    ) {
        MergeMethodResult result = new MergeMethodResult();
        for (MethodDeclaration sourceMethod : sourceClass.getMethods()) {
            String signature = methodSignature(sourceMethod);
            if (!methodSignatures.contains(signature)) {
                continue;
            }
            MethodDeclaration existingMethod = findMethodBySignature(destClass, signature);
            if (existingMethod == null) {
                insertMember(destClass, sourceMethod.clone(), MemberInsertPosition.METHOD);
                result.added++;
            } else {
                existingMethod.replace(sourceMethod.clone());
                result.overwritten++;
            }
        }
        return result;
    }

    private void insertMember(
            ClassOrInterfaceDeclaration clazz,
            BodyDeclaration<?> member,
            MemberInsertPosition position
    ) {
        NodeList<BodyDeclaration<?>> members = clazz.getMembers();
        int index = members.size();

        if (position == MemberInsertPosition.FIELD) {
            index = firstCallableIndex(members);
        } else if (position == MemberInsertPosition.CONSTRUCTOR) {
            index = firstMethodIndex(members);
        }
        members.add(index, member);
    }

    private int firstCallableIndex(NodeList<BodyDeclaration<?>> members) {
        for (int i = 0; i < members.size(); i++) {
            BodyDeclaration<?> member = members.get(i);
            if (member.isConstructorDeclaration() || member.isMethodDeclaration()) {
                return i;
            }
        }
        return members.size();
    }

    private int firstMethodIndex(NodeList<BodyDeclaration<?>> members) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).isMethodDeclaration()) {
                return i;
            }
        }
        return members.size();
    }

    private void writeValidJavaOrFallback(
            String fullClassName,
            Path destFile,
            String generatedText
    ) throws IOException {
        if (isValidJavaSource(generatedText, "NEW " + fullClassName)) {
            Files.write(destFile, generatedText.getBytes(StandardCharsets.UTF_8));
            return;
        }

        String rawText = sourceIndex.rawTextByClass.get(fullClassName);
        if (rawText == null || !isValidJavaSource(rawText, "RAW " + fullClassName)) {
            throw new IOException("Generated source is invalid and no valid raw fallback is available: "
                    + fullClassName);
        }
        Files.write(destFile, rawText.getBytes(StandardCharsets.UTF_8));
        JavaParseLogUtils.logWarn("Fallback to raw source copy: " + fullClassName);
    }

    private boolean isValidJavaSource(String source, String context) {
        try {
            StaticJavaParser.parse(source);
            return true;
        } catch (Exception ex) {
            JavaParseLogUtils.logWarn("Invalid Java source [" + context + "]: " + ex.getMessage());
            return false;
        }
    }

    private void setupLexicalPreservation(CompilationUnit cu) {
        try {
            LexicalPreservingPrinter.setup(cu);
        } catch (Exception ignored) {
            // Fall back to JavaParser's regular printer when lexical setup is not possible.
        }
    }

    private String printCompilationUnit(CompilationUnit cu) {
        try {
            return LexicalPreservingPrinter.print(cu);
        } catch (Exception ignored) {
            return cu.toString();
        }
    }

    private int syncMapperXml(String fullClassName, Set<String> methodSignatures) throws IOException {
        String relPath = mapperXmlIndex.relPathByNamespace.get(fullClassName);
        if (relPath == null) {
            return 0;
        }

        LinkedHashMap<String, String> fragments = resolveMapperFragments(fullClassName, methodSignatures);
        if (fragments.isEmpty()) {
            JavaParseLogUtils.logWarn("No mapper SQL fragments found for " + fullClassName);
            return 0;
        }

        Path destXmlFile = destResourcesRoot().resolve(relPath).normalize();
        Files.createDirectories(destXmlFile.getParent());

        if (!Files.exists(destXmlFile)) {
            String xml = buildNewMapperXml(fullClassName, fragments);
            Files.write(destXmlFile, xml.getBytes(StandardCharsets.UTF_8));
            JavaParseLogUtils.logInfo("XML NEW (" + fragments.size() + " fragments): " + relPath);
            return fragments.size();
        }

        String destXml = new String(Files.readAllBytes(destXmlFile), StandardCharsets.UTF_8);
        Set<String> existingIds = readXmlElementIds(destXml);
        int synced = 0;

        for (Map.Entry<String, String> entry : fragments.entrySet()) {
            String id = idFromXmlKey(entry.getKey());
            if (existingIds.contains(id) || xmlContainsId(destXml, id)) {
                continue;
            }
            destXml = insertBeforeMapperEnd(destXml, indentXmlFragment(entry.getValue()));
            existingIds.add(id);
            synced++;
        }

        if (synced > 0) {
            Files.write(destXmlFile, destXml.getBytes(StandardCharsets.UTF_8));
            JavaParseLogUtils.logInfo("XML MERGE (" + synced + " fragments): " + relPath);
        }
        return synced;
    }

    private LinkedHashMap<String, String> resolveMapperFragments(
            String namespace,
            Set<String> methodSignatures
    ) {
        LinkedHashMap<String, String> fragments = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();

        for (String signature : methodSignatures) {
            String methodId = extractMethodIdFromSignature(signature);
            String statementKey = namespace + "." + methodId;
            addXmlDependencies(statementKey, fragments, visiting);
            String statement = mapperXmlIndex.statementSqlByKey.get(statementKey);
            if (statement != null) {
                fragments.put(statementKey, statement);
            }
        }
        return fragments;
    }

    private void addXmlDependencies(
            String key,
            LinkedHashMap<String, String> fragments,
            Set<String> visiting
    ) {
        if (!visiting.add(key)) {
            return;
        }
        Set<String> dependencies = mapperXmlIndex.dependenciesByKey.get(key);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                addXmlDependencies(dependency, fragments, visiting);
                String support = mapperXmlIndex.supportSqlByKey.get(dependency);
                if (support != null) {
                    fragments.put(dependency, support);
                }
            }
        }
        visiting.remove(key);
    }

    private String buildNewMapperXml(String namespace, LinkedHashMap<String, String> fragments) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" ")
                .append("\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n")
                .append("<mapper namespace=\"").append(namespace).append("\">\n");

        for (String fragment : fragments.values()) {
            sb.append(indentXmlFragment(fragment));
        }
        sb.append("</mapper>\n");
        return sb.toString();
    }

    private Set<String> readXmlElementIds(String xml) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        try {
            Document doc = parseXml(xml);
            Element root = doc.getDocumentElement();
            if (root == null) {
                return ids;
            }
            org.w3c.dom.NodeList nodes = root.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node node = nodes.item(i);
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                    continue;
                }
                String id = trim(((Element) node).getAttribute("id"));
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
        } catch (Exception ex) {
            JavaParseLogUtils.logWarn("Failed to parse destination mapper xml, fallback to text check: "
                    + ex.getMessage());
        }
        return ids;
    }

    private String insertBeforeMapperEnd(String xml, String fragment) {
        int index = xml.lastIndexOf("</mapper>");
        if (index < 0) {
            return xml + fragment;
        }
        return xml.substring(0, index) + fragment + xml.substring(index);
    }

    private String indentXmlFragment(String fragment) {
        String normalized = fragment == null ? "" : fragment.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return "\n    " + normalized.replace("\n", "\n    ") + "\n";
    }

    private boolean xmlContainsId(String xml, String id) {
        return xml.contains("id=\"" + id + "\"") || xml.contains("id='" + id + "'");
    }

    private String idFromXmlKey(String key) {
        int index = key.lastIndexOf('.');
        return index >= 0 ? key.substring(index + 1) : key;
    }

    private String extractMethodIdFromSignature(String signature) {
        int paren = signature.indexOf('(');
        return paren > 0 ? signature.substring(0, paren) : signature;
    }

    private void printSummary() {
        JavaParseLogUtils.logInfo("---------- Migration Summary ----------");
        JavaParseLogUtils.logInfo("Total types to migrate: " + migrationPlan.migrationTypes.size());
        for (String type : migrationPlan.migrationTypes) {
            Set<String> methods = migrationPlan.usedMethods.get(type);
            String category = summaryCategory(type, methods);
            Path sourcePath = sourceIndex.sourceFileByClass.get(type);
            JavaParseLogUtils.logInfo("  [" + category + "] " + type
                    + (sourcePath == null ? " (not found)" : " -> " + relativizeSourcePath(sourcePath)));
        }
    }

    private String summaryCategory(String fullClassName, Set<String> methods) {
        if (isMapperClass(fullClassName)) {
            return "MAPPER(" + (methods == null ? 0 : methods.size()) + "m)";
        }
        if (isEnum(fullClassName)) {
            return "ENUM";
        }
        if (isAnnotation(fullClassName)) {
            return "ANNOTATION";
        }
        if (isModel(fullClassName)) {
            return "MODEL";
        }
        if (isInterface(fullClassName)) {
            return "INTERFACE";
        }
        return methods != null && !methods.isEmpty()
                ? "CALL_CHAIN(" + methods.size() + "m)"
                : "TYPE_REF";
    }

    private boolean shouldCopyWholeType(String fullClassName) {
        return isModel(fullClassName) || isEnum(fullClassName) || isAnnotation(fullClassName);
    }

    private boolean isModel(String fullClassName) {
        TypeDeclaration<?> declaration = sourceIndex.declarationByClass.get(fullClassName);
        if (!(declaration instanceof ClassOrInterfaceDeclaration)) {
            return false;
        }
        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration;
        if (clazz.isInterface()) {
            return false;
        }

        String simpleName = shortName(fullClassName);
        for (String suffix : MODEL_SUFFIXES) {
            if (simpleName.endsWith(suffix)) {
                return true;
            }
        }

        boolean hasModelAnnotation = false;
        boolean hasComponentAnnotation = false;
        for (AnnotationExpr annotation : clazz.getAnnotations()) {
            String name = annotation.getNameAsString();
            hasModelAnnotation |= MODEL_ANNOTATIONS.contains(name);
            hasComponentAnnotation |= COMPONENT_ANNOTATIONS.contains(name);
        }
        return hasModelAnnotation && !hasComponentAnnotation;
    }

    private boolean isEnum(String fullClassName) {
        TypeDeclaration<?> declaration = sourceIndex.declarationByClass.get(fullClassName);
        return declaration instanceof EnumDeclaration || declaration != null && declaration.isEnumDeclaration();
    }

    private boolean isAnnotation(String fullClassName) {
        TypeDeclaration<?> declaration = sourceIndex.declarationByClass.get(fullClassName);
        return declaration instanceof AnnotationDeclaration;
    }

    private boolean isInterface(String fullClassName) {
        TypeDeclaration<?> declaration = sourceIndex.declarationByClass.get(fullClassName);
        return declaration instanceof ClassOrInterfaceDeclaration
                && ((ClassOrInterfaceDeclaration) declaration).isInterface();
    }

    private boolean isMapperClass(String fullClassName) {
        TypeDeclaration<?> declaration = sourceIndex.declarationByClass.get(fullClassName);
        if (declaration == null) {
            return false;
        }
        for (AnnotationExpr annotation : declaration.getAnnotations()) {
            String name = annotation.getNameAsString();
            if ("Mapper".equals(name) || name.endsWith(".Mapper")) {
                return true;
            }
        }
        return shortName(fullClassName).endsWith("Mapper");
    }

    private List<String> resolveEntryClassCandidates(String className) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalized = normalizeTypeName(className);
        if (normalized == null || normalized.isEmpty()) {
            return new ArrayList<>();
        }

        if (normalized.contains(".")) {
            if (sourceIndex.allClasses.contains(normalized)
                    && sourceIndex.hasMethodName(normalized, JavaCodeMigratorConfig.ENTRY_METHOD_NAME)) {
                result.add(normalized);
            }
        } else {
            Set<String> candidates = sourceIndex.simpleNameToFullClass.get(normalized);
            if (candidates != null) {
                for (String candidate : candidates) {
                    if (sourceIndex.hasMethodName(candidate, JavaCodeMigratorConfig.ENTRY_METHOD_NAME)) {
                        result.add(candidate);
                    }
                }
                if (result.isEmpty()) {
                    result.addAll(candidates);
                }
            }
        }

        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    private List<String> resolveClassCandidates(String typeName, String contextClass) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalized = normalizeTypeName(typeName);
        if (normalized == null || normalized.isEmpty()) {
            return new ArrayList<>();
        }

        String simpleName = shortName(normalized);
        if (normalized.contains(".")) {
            if (sourceIndex.allClasses.contains(normalized)) {
                result.add(normalized);
            }
            if (contextClass != null) {
                String pkg = sourceIndex.packageByClass.get(contextClass);
                String samePackageName = pkg == null || pkg.isEmpty()
                        ? normalized
                        : pkg + "." + normalized;
                if (sourceIndex.allClasses.contains(samePackageName)) {
                    result.add(samePackageName);
                }
            }
            if (result.isEmpty()) {
                result.add(normalized);
            }
            return new ArrayList<>(result);
        }

        if (contextClass != null) {
            Map<String, String> explicitImports = sourceIndex.explicitImportsByClass.get(contextClass);
            if (explicitImports != null) {
                String imported = explicitImports.get(simpleName);
                if (imported != null && !imported.isEmpty()) {
                    result.add(imported);
                    return new ArrayList<>(result);
                }
            }

            String pkg = sourceIndex.packageByClass.get(contextClass);
            if (pkg != null && !pkg.isEmpty()) {
                String samePackageName = pkg + "." + simpleName;
                if (sourceIndex.allClasses.contains(samePackageName)) {
                    result.add(samePackageName);
                }
            }

            List<String> onDemandImports = sourceIndex.onDemandImportsByClass.get(contextClass);
            if (onDemandImports != null) {
                for (String importPackage : onDemandImports) {
                    if (importPackage == null || importPackage.isEmpty()) {
                        continue;
                    }
                    String candidate = importPackage + "." + simpleName;
                    if (sourceIndex.allClasses.contains(candidate)) {
                        result.add(candidate);
                    }
                }
            }

            if (!result.isEmpty()) {
                return new ArrayList<>(result);
            }
        }

        Set<String> globalCandidates = sourceIndex.simpleNameToFullClass.get(simpleName);
        if (globalCandidates != null) {
            result.addAll(globalCandidates);
        }
        return new ArrayList<>(result);
    }

    private MethodDeclaration resolveMethod(
            String className,
            String methodName,
            Integer argCount,
            List<String> argTypeHints
    ) {
        List<MethodDeclaration> candidates = sourceIndex.methodsByClassAndName.get(methodKey(className, methodName));
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

        List<MethodDeclaration> exact = new ArrayList<>();
        MethodDeclaration bestVarArgs = null;
        int bestVarArgsParamCount = -1;
        for (MethodDeclaration candidate : candidates) {
            int paramCount = candidate.getParameters().size();
            boolean varArgs = paramCount > 0 && candidate.getParameter(paramCount - 1).isVarArgs();
            if (!varArgs && paramCount == argCount) {
                exact.add(candidate);
            } else if (varArgs && argCount >= paramCount - 1
                    && paramCount > bestVarArgsParamCount) {
                bestVarArgs = candidate;
                bestVarArgsParamCount = paramCount;
            }
        }

        if (!exact.isEmpty()) {
            if (exact.size() == 1) {
                return exact.get(0);
            }
            MethodDeclaration best = exact.get(0);
            int bestScore = Integer.MIN_VALUE;
            for (MethodDeclaration candidate : exact) {
                int score = scoreOverload(candidate, argTypeHints);
                if (score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
            return best;
        }
        return bestVarArgs;
    }

    private List<String> inferCallArgTypeHints(
            MethodCallExpr call,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap
    ) {
        List<String> hints = new ArrayList<>();
        call.getArguments().forEach(argument ->
                hints.add(inferExpressionType(argument, localVarTypeMap, fieldMap)));
        return hints;
    }

    private String inferExpressionType(
            Expression expression,
            Map<String, String> localVarTypeMap,
            Map<String, String> fieldMap
    ) {
        if (expression == null) {
            return null;
        }
        if (expression.isNullLiteralExpr()) {
            return "null";
        }
        if (expression.isStringLiteralExpr()) {
            return "String";
        }
        if (expression.isBooleanLiteralExpr()) {
            return "boolean";
        }
        if (expression.isCharLiteralExpr()) {
            return "char";
        }
        if (expression.isIntegerLiteralExpr()) {
            return "int";
        }
        if (expression.isLongLiteralExpr()) {
            return "long";
        }
        if (expression.isDoubleLiteralExpr()) {
            return "double";
        }
        if (expression.isObjectCreationExpr()) {
            return normalizeTypeName(expression.asObjectCreationExpr().getType().asString());
        }
        if (expression.isCastExpr()) {
            return normalizeTypeName(expression.asCastExpr().getType().asString());
        }
        if (expression.isNameExpr()) {
            String name = expression.asNameExpr().getNameAsString();
            String localType = normalizeTypeName(localVarTypeMap.get(name));
            return localType != null ? localType : normalizeTypeName(fieldMap.get(name));
        }
        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccess = expression.asFieldAccessExpr();
            String scopeVar = normalizeScopeVar(fieldAccess.getScope().toString());
            String scopedType = normalizeTypeName(fieldMap.get(scopeVar));
            if (scopedType != null) {
                return scopedType;
            }
            return normalizeTypeName(fieldMap.get(fieldAccess.getNameAsString()));
        }
        return null;
    }

    private int scoreOverload(MethodDeclaration candidate, List<String> argTypeHints) {
        if (argTypeHints == null || argTypeHints.isEmpty()) {
            return 0;
        }

        int score = 0;
        int count = Math.min(candidate.getParameters().size(), argTypeHints.size());
        for (int i = 0; i < count; i++) {
            String parameterType = normalizeTypeName(candidate.getParameter(i).getType().asString());
            String argumentType = normalizeTypeName(argTypeHints.get(i));
            if (argumentType == null) {
                continue;
            }
            if ("null".equals(argumentType)) {
                score += isPrimitiveType(parameterType) ? -50 : 1;
                continue;
            }
            String parameterShortName = shortName(parameterType);
            String argumentShortName = shortName(argumentType);
            if (parameterType != null && argumentType != null
                    && (parameterType.equals(argumentType)
                    || parameterShortName.equals(argumentShortName))) {
                score += 8;
            } else if (isPrimitiveWrapperPair(parameterShortName, argumentShortName)) {
                score += 6;
            } else if ("Object".equals(parameterShortName)) {
                score += 1;
            } else {
                score -= 4;
            }
        }
        return score;
    }

    private void collectTargetsByType(
            LinkedHashSet<String> targets,
            String typeName,
            String methodName,
            Integer argCount,
            List<String> argTypeHints,
            String contextClass
    ) {
        String normalized = normalizeTypeName(typeName);
        if (normalized == null) {
            return;
        }

        List<String> candidates = resolveClassCandidates(normalized, contextClass);
        boolean implementationMatched = false;
        for (String candidate : candidates) {
            Set<String> impls = sourceIndex.interfaceToImplementations.get(candidate);
            if (impls == null || impls.isEmpty()) {
                continue;
            }
            implementationMatched = true;
            for (String impl : impls) {
                if (resolveMethod(impl, methodName, argCount, argTypeHints) != null) {
                    targets.add(impl);
                }
            }
        }

        if (!implementationMatched) {
            Set<String> impls = sourceIndex.interfaceToImplementations.get(normalized);
            if (impls != null) {
                for (String impl : impls) {
                    if (resolveMethod(impl, methodName, argCount, argTypeHints) != null) {
                        targets.add(impl);
                        implementationMatched = true;
                    }
                }
            }
        }

        if (!implementationMatched) {
            for (String candidate : candidates) {
                if (resolveMethod(candidate, methodName, argCount, argTypeHints) != null) {
                    targets.add(candidate);
                }
            }
        }
    }

    private static String methodSignature(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder(method.getNameAsString()).append("(");
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(cleanExpr(method.getParameter(i).getType().asString()));
        }
        return sb.append(")").toString();
    }

    private static String constructorSignature(ConstructorDeclaration constructor) {
        StringBuilder sb = new StringBuilder("<init>(");
        for (int i = 0; i < constructor.getParameters().size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(cleanExpr(constructor.getParameter(i).getType().asString()));
        }
        return sb.append(")").toString();
    }

    private MethodDeclaration findMethodBySignature(
            ClassOrInterfaceDeclaration clazz,
            String signature
    ) {
        for (MethodDeclaration method : clazz.getMethods()) {
            if (methodSignature(method).equals(signature)) {
                return method;
            }
        }
        return null;
    }

    private TypeDeclaration<?> findTypeDeclaration(CompilationUnit cu, String fullClassName) {
        if (cu == null) {
            return null;
        }
        String pkg = packageName(cu);
        for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
            if (fullClassName.equals(toFullClassName(pkg, typeDeclaration.getNameAsString()))) {
                return typeDeclaration;
            }
        }
        return null;
    }

    private String packageName(CompilationUnit cu) {
        return cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
    }

    private String toFullClassName(String pkg, String simpleName) {
        return pkg == null || pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
    }

    private String fullTypeName(String pkg, TypeDeclaration<?> type) {
        Deque<String> names = new ArrayDeque<>();
        Node current = type;
        while (current instanceof TypeDeclaration) {
            TypeDeclaration<?> currentType = (TypeDeclaration<?>) current;
            names.addFirst(currentType.getNameAsString());
            Optional<Node> parent = current.getParentNode();
            if (!parent.isPresent()) {
                break;
            }
            current = parent.get();
        }
        String joined = String.join(".", names);
        return pkg == null || pkg.isEmpty() ? joined : pkg + "." + joined;
    }

    private String methodKey(String className, String methodName) {
        return className + "." + methodName;
    }

    private String relativizeSourcePath(Path path) {
        try {
            return normalizePath(sourceRoot().relativize(path.toAbsolutePath().normalize()).toString());
        } catch (Exception ex) {
            return normalizePath(path.toString());
        }
    }

    private String relativeDestPath(Path path) {
        try {
            return normalizePath(destRoot().relativize(path.toAbsolutePath().normalize()).toString());
        } catch (Exception ex) {
            return normalizePath(path.toString());
        }
    }

    private static boolean isJavaFile(Path path) {
        return path.getFileName().toString().endsWith(JavaCodeMigratorConfig.JAVA_SUFFIX);
    }

    private static boolean hasUsedMethods(Set<String> methods) {
        return methods != null && !methods.isEmpty();
    }

    private static void addAll(Set<String> target, Set<String> source) {
        if (source != null) {
            target.addAll(source);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeTypeName(String value) {
        return JavaParseTextUtils.normalizeTypeName(value);
    }

    private static String shortName(String value) {
        return JavaParseTextUtils.shortClassName(value);
    }

    private static String cleanExpr(String value) {
        return JavaParseTextUtils.normalizeInlineWhitespace(value);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeScopeVar(String value) {
        return JavaParseTextUtils.normalizeScopeVar(value);
    }

    private static boolean isTypeLikeScope(String value) {
        return JavaParseTextUtils.isTypeLikeScope(value);
    }

    private static String normalizePath(String value) {
        return JavaParseTextUtils.normalizePathSlash(value);
    }

    private static boolean isPrimitiveType(String value) {
        if (value == null) {
            return false;
        }
        switch (value) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "boolean":
            case "char":
                return true;
            default:
                return false;
        }
    }

    private static boolean isPrimitiveWrapperPair(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return ("byte".equals(left) && "Byte".equals(right)) || ("Byte".equals(left) && "byte".equals(right))
                || ("short".equals(left) && "Short".equals(right)) || ("Short".equals(left) && "short".equals(right))
                || ("int".equals(left) && "Integer".equals(right)) || ("Integer".equals(left) && "int".equals(right))
                || ("long".equals(left) && "Long".equals(right)) || ("Long".equals(left) && "long".equals(right))
                || ("float".equals(left) && "Float".equals(right)) || ("Float".equals(left) && "float".equals(right))
                || ("double".equals(left) && "Double".equals(right)) || ("Double".equals(left) && "double".equals(right))
                || ("boolean".equals(left) && "Boolean".equals(right)) || ("Boolean".equals(left) && "boolean".equals(right))
                || ("char".equals(left) && "Character".equals(right)) || ("Character".equals(left) && "char".equals(right));
    }

    private static boolean isJdkOrThirdPartyType(String value) {
        if (value == null) {
            return true;
        }
        for (String prefix : JavaCodeMigratorConfig.JDK_PREFIXES) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        for (String prefix : JavaCodeMigratorConfig.THIRD_PARTY_PREFIXES) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private enum MemberInsertPosition {
        FIELD,
        CONSTRUCTOR,
        METHOD
    }

    private static final class SourceIndex {
        private final Map<String, List<MethodDeclaration>> methodsByClassAndName = new HashMap<>();
        private final Map<String, Map<String, String>> fieldTypesByClass = new HashMap<>();
        private final Map<String, String> packageByClass = new HashMap<>();
        private final Map<String, Map<String, String>> explicitImportsByClass = new HashMap<>();
        private final Map<String, List<String>> onDemandImportsByClass = new HashMap<>();
        private final Map<String, Set<String>> simpleNameToFullClass = new HashMap<>();
        private final Map<String, Set<String>> interfaceToImplementations = new HashMap<>();
        private final Map<String, Path> sourceFileByClass = new HashMap<>();
        private final Map<String, TypeDeclaration<?>> declarationByClass = new HashMap<>();
        private final Map<String, CompilationUnit> compilationUnitByClass = new HashMap<>();
        private final Map<String, String> rawTextByClass = new HashMap<>();
        private final Set<String> allClasses = new HashSet<>();

        private void indexTopLevelType(
                String fullName,
                String pkg,
                TypeDeclaration<?> declaration,
                CompilationUnit cu,
                String rawText,
                Path sourceFile,
                JavaMethodCallAnalysisUtils.ImportContext importContext
        ) {
            allClasses.add(fullName);
            simpleNameToFullClass.computeIfAbsent(declaration.getNameAsString(),
                    key -> new LinkedHashSet<>()).add(fullName);
            packageByClass.put(fullName, pkg);
            explicitImportsByClass.put(fullName, new HashMap<>(importContext.getExplicitImports()));
            onDemandImportsByClass.put(fullName, new ArrayList<>(importContext.getOnDemandImports()));
            sourceFileByClass.put(fullName, sourceFile);
            declarationByClass.put(fullName, declaration);
            compilationUnitByClass.put(fullName, cu);
            rawTextByClass.put(fullName, rawText);

            if (declaration instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration;
                clazz.getMethods().forEach(method ->
                        methodsByClassAndName.computeIfAbsent(fullName + "." + method.getNameAsString(),
                                key -> new ArrayList<>()).add(method));
                fieldTypesByClass.put(fullName, collectFieldTypes(clazz));
            } else if (declaration instanceof EnumDeclaration) {
                fieldTypesByClass.put(fullName, collectFieldTypes(declaration));
            }
        }

        private Map<String, String> collectFieldTypes(TypeDeclaration<?> declaration) {
            Map<String, String> fieldTypes = new HashMap<>();
            declaration.getFields().forEach(field -> {
                String type = field.getElementType().asString();
                field.getVariables().forEach(variable ->
                        fieldTypes.put(variable.getNameAsString(), type));
            });
            return fieldTypes;
        }

        private void buildInterfaceImplementationIndex(JavaCodeMigrator migrator) {
            interfaceToImplementations.clear();
            for (Map.Entry<String, TypeDeclaration<?>> entry : declarationByClass.entrySet()) {
                String fullClassName = entry.getKey();
                TypeDeclaration<?> declaration = entry.getValue();
                if (!(declaration instanceof ClassOrInterfaceDeclaration)) {
                    continue;
                }
                ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) declaration;
                if (clazz.isInterface()) {
                    continue;
                }
                for (ClassOrInterfaceType implementedType : clazz.getImplementedTypes()) {
                    registerImplementation(migrator, fullClassName, implementedType.asString());
                }
            }
        }

        private void registerImplementation(
                JavaCodeMigrator migrator,
                String implClassName,
                String interfaceTypeName
        ) {
            String normalized = normalizeTypeName(interfaceTypeName);
            if (normalized == null || normalized.isEmpty()) {
                return;
            }
            addInterfaceImpl(normalized, implClassName);
            addInterfaceImpl(shortName(normalized), implClassName);
            for (String candidate : migrator.resolveClassCandidates(normalized, implClassName)) {
                addInterfaceImpl(candidate, implClassName);
                addInterfaceImpl(shortName(candidate), implClassName);
            }
        }

        private void addInterfaceImpl(String interfaceName, String implClassName) {
            if (interfaceName == null || interfaceName.isEmpty()) {
                return;
            }
            interfaceToImplementations.computeIfAbsent(interfaceName,
                    key -> new LinkedHashSet<>()).add(implClassName);
        }

        private boolean hasMethodName(String className, String methodName) {
            return methodsByClassAndName.containsKey(className + "." + methodName);
        }

        private int indexedMethodCount() {
            int count = 0;
            for (List<MethodDeclaration> methods : methodsByClassAndName.values()) {
                count += methods.size();
            }
            return count;
        }
    }

    private static final class MapperXmlIndex {
        private final Map<String, String> statementSqlByKey = new LinkedHashMap<>();
        private final Map<String, String> supportSqlByKey = new LinkedHashMap<>();
        private final Map<String, Set<String>> dependenciesByKey = new HashMap<>();
        private final Map<String, Path> sourceFileByNamespace = new HashMap<>();
        private final Map<String, String> rawXmlByNamespace = new HashMap<>();
        private final Map<String, String> relPathByNamespace = new HashMap<>();

        private void indexMapper(String namespace, Path sourceFile, String relPath, String rawXml) {
            sourceFileByNamespace.put(namespace, sourceFile);
            relPathByNamespace.put(namespace, relPath);
            rawXmlByNamespace.put(namespace, rawXml);
        }
    }

    private static final class MigrationPlan {
        private final Set<String> migrationTypes = new LinkedHashSet<>();
        private final Set<String> expandedMethodKeys = new HashSet<>();
        private final Set<String> analyzedTypes = new HashSet<>();
        private final Map<String, Set<String>> usedMethods = new LinkedHashMap<>();

        private void recordUsedMethod(String fullClassName, MethodDeclaration method) {
            usedMethods.computeIfAbsent(fullClassName, key -> new LinkedHashSet<>())
                    .add(JavaCodeMigrator.methodSignature(method));
        }
    }

    private static final class MigrationStats {
        private int wholeCopied;
        private int wholeSkipped;
        private int methodMerged;
        private int methodNewFile;
        private int xmlSynced;
        private int failed;
        private int callChainSkipped;
    }

    private static final class MergeMethodResult {
        private int added;
        private int overwritten;
    }
}
