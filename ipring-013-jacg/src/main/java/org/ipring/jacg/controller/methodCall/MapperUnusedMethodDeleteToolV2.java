package org.ipring.jacg.controller.methodCall;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;
import org.ipring.jacg.util.CsvIOUtils;
import org.ipring.jacg.util.JavaParseLogUtils;
import org.ipring.jacg.util.JavaParseTextUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapperUnusedMethodDeleteToolV2 {

    private static final List<String> SQL_TAGS = Arrays.asList("select", "insert", "update", "delete");
    private static final String JAVA_SUFFIX = ".java";
    private static final String XML_SUFFIX = ".xml";

    @Data
    static class RuntimeConfig {
        String projectRoot;
        String csvPath;

        static RuntimeConfig of(String projectRoot, String csvPath) {
            RuntimeConfig config = new RuntimeConfig();
            config.setProjectRoot(projectRoot);
            config.setCsvPath(csvPath);
            return config;
        }
    }

    static class UnusedMethodRecord {
        final String mapperFullName;
        final String methodName;
        final int paramCount;
        final String sourceFile;
        final int lineNo;

        UnusedMethodRecord(String mapperFullName, String methodName, int paramCount, String sourceFile, int lineNo) {
            this.mapperFullName = mapperFullName;
            this.methodName = methodName;
            this.paramCount = paramCount;
            this.sourceFile = sourceFile;
            this.lineNo = lineNo;
        }
    }

    static class DeleteTargetKey {
        final String mapperFullName;
        final String methodName;
        final int paramCount;

        DeleteTargetKey(String mapperFullName, String methodName, int paramCount) {
            this.mapperFullName = mapperFullName;
            this.methodName = methodName;
            this.paramCount = paramCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DeleteTargetKey)) {
                return false;
            }
            DeleteTargetKey other = (DeleteTargetKey) obj;
            return paramCount == other.paramCount
                    && mapperFullName.equals(other.mapperFullName)
                    && methodName.equals(other.methodName);
        }

        @Override
        public int hashCode() {
            int result = mapperFullName.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + paramCount;
            return result;
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

    static class DeleteSummary {
        int javaFilesTouched;
        int javaMethodsDeleted;
        int xmlFilesTouched;
        int xmlStatementsDeleted;
        int methodsNotFoundInJava;
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
        RuntimeConfig config = RuntimeConfig.of("D:\\project\\dbu-mod-waybill", "unused_mapper_methods.csv");
        Path projectRoot = Paths.get(config.projectRoot).toAbsolutePath().normalize();
        Path csvPath = Paths.get(config.csvPath).toAbsolutePath().normalize();

        JavaParseLogUtils.logInfo("Tool start. projectRoot=" + projectRoot + ", csvPath=" + csvPath);
        List<UnusedMethodRecord> records = loadUnusedRecords(csvPath);
        if (records.isEmpty()) {
            JavaParseLogUtils.logWarn("No unused mapper methods found in csv: " + csvPath);
            return;
        }

        DeleteSummary summary = new DeleteSummary();
        deleteMapperMethodsInJava(projectRoot, records, summary);
        deleteMapperStatementsInXml(projectRoot, records, summary);

        JavaParseLogUtils.logInfo(String.format(
                Locale.ROOT,
                "Done. javaFilesTouched=%d, javaMethodsDeleted=%d, xmlFilesTouched=%d, xmlStatementsDeleted=%d, methodsNotFoundInJava=%d",
                summary.javaFilesTouched,
                summary.javaMethodsDeleted,
                summary.xmlFilesTouched,
                summary.xmlStatementsDeleted,
                summary.methodsNotFoundInJava
        ));
    }

    private static List<UnusedMethodRecord> loadUnusedRecords(Path csvPath) throws Exception {
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException("CSV file not found: " + csvPath);
        }
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<UnusedMethodRecord> records = new ArrayList<>();
        int start = 0;
        if (lines.get(0).toLowerCase(Locale.ROOT).contains("mapper_interface")) {
            start = 1;
        }

        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (JavaParseTextUtils.normalizeInlineWhitespace(line).isEmpty()) {
                continue;
            }
            List<String> fields = CsvIOUtils.parseCsvLine(line);
            if (fields.size() < 6) {
                JavaParseLogUtils.logWarn("Skip malformed csv line " + (i + 1) + ": " + line);
                continue;
            }

            String mapper = fields.get(1);
            String methodName = fields.get(2);
            int paramCount = parseInt(fields.get(3), 0);
            String sourceFile = JavaParseTextUtils.normalizePathSlash(fields.get(4));
            int lineNo = parseInt(fields.get(5), -1);

            if (JavaParseTextUtils.normalizeInlineWhitespace(mapper).isEmpty() || JavaParseTextUtils.normalizeInlineWhitespace(methodName).isEmpty() || JavaParseTextUtils.normalizeInlineWhitespace(sourceFile).isEmpty()) {
                JavaParseLogUtils.logWarn("Skip incomplete csv line " + (i + 1) + ": " + line);
                continue;
            }

            records.add(new UnusedMethodRecord(mapper, methodName, paramCount, sourceFile, lineNo));
        }
        return records;
    }

    private static void deleteMapperMethodsInJava(
            Path projectRoot,
            List<UnusedMethodRecord> records,
            DeleteSummary summary
    ) throws Exception {
        Map<String, List<UnusedMethodRecord>> fileRecordMap = new HashMap<>();
        for (UnusedMethodRecord record : records) {
            fileRecordMap.computeIfAbsent(record.sourceFile, key -> new ArrayList<>()).add(record);
        }

        List<String> sourceFiles = new ArrayList<>(fileRecordMap.keySet());
        sourceFiles.sort(String::compareTo);

        for (String sourceFile : sourceFiles) {
            Path javaPath = projectRoot.resolve(sourceFile.replace('/', java.io.File.separatorChar)).normalize();
            if (!Files.exists(javaPath)) {
                JavaParseLogUtils.logWarn("Java file not found, skip: " + javaPath);
                continue;
            }
            if (!javaPath.getFileName().toString().endsWith(JAVA_SUFFIX)) {
                continue;
            }

            int deletedCount = deleteMethodsInSingleJavaFile(javaPath, fileRecordMap.get(sourceFile), summary);
            if (deletedCount > 0) {
                summary.javaFilesTouched++;
                summary.javaMethodsDeleted += deletedCount;
            }
        }
    }

    private static int deleteMethodsInSingleJavaFile(
            Path javaPath,
            List<UnusedMethodRecord> records,
            DeleteSummary summary
    ) throws Exception {
        List<String> lines = Files.readAllLines(javaPath, StandardCharsets.UTF_8);
        CompilationUnit cu = StaticJavaParser.parse(javaPath);
        String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        Set<DeleteTargetKey> deduplicated = new LinkedHashSet<>();
        for (UnusedMethodRecord record : records) {
            deduplicated.add(new DeleteTargetKey(record.mapperFullName, record.methodName, record.paramCount));
        }

        List<LineRange> ranges = new ArrayList<>();
        int found = 0;
        for (DeleteTargetKey key : deduplicated) {
            MethodDeclaration method = findTargetMethod(cu, pkg, key);
            if (method == null) {
                summary.methodsNotFoundInJava++;
                JavaParseLogUtils.logWarn("Method not found in java: " + key.mapperFullName + "." + key.methodName + "(" + key.paramCount + ")");
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

        List<LineRange> mergedRanges = mergeRanges(ranges);
        for (int i = mergedRanges.size() - 1; i >= 0; i--) {
            LineRange range = mergedRanges.get(i);
            int from = Math.max(0, range.startLine - 1);
            int to = Math.min(lines.size(), range.endLine);
            if (from < to) {
                lines.subList(from, to).clear();
            }
        }

        CsvIOUtils.writeLines(javaPath, lines);
        JavaParseLogUtils.logInfo("Java updated: " + javaPath + ", deletedMethods=" + found);
        return found;
    }

    private static MethodDeclaration findTargetMethod(
            CompilationUnit cu,
            String pkg,
            DeleteTargetKey key
    ) {
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration clazz : classes) {
            if (!clazz.isInterface()) {
                continue;
            }
            String fullName = pkg.isEmpty() ? clazz.getNameAsString() : pkg + "." + clazz.getNameAsString();
            if (!key.mapperFullName.equals(fullName)) {
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
        ranges.sort(Comparator
                .comparingInt((LineRange item) -> item.startLine)
                .thenComparingInt(item -> item.endLine));

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

    private static void deleteMapperStatementsInXml(
            Path projectRoot,
            List<UnusedMethodRecord> records,
            DeleteSummary summary
    ) throws Exception {
        Map<String, Set<String>> mapperMethodIdMap = new HashMap<>();
        for (UnusedMethodRecord record : records) {
            mapperMethodIdMap.computeIfAbsent(record.mapperFullName, key -> new HashSet<>()).add(record.methodName);
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
                        summary.xmlFilesTouched++;
                        summary.xmlStatementsDeleted += removed;
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
                CsvIOUtils.writeUtf8(xmlFile, updated);
                JavaParseLogUtils.logInfo("XML updated: " + xmlFile + ", deletedStatements=" + removed);
            }
            return removed;
        } catch (Exception ex) {
            JavaParseLogUtils.logWarn("Skip xml parse/write error: " + xmlFile + " | " + JavaParseTextUtils.normalizeInlineWhitespace(ex.getMessage()));
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
        return JavaParseTextUtils.normalizeInlineWhitespace(namespace);
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

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(JavaParseTextUtils.normalizeInlineWhitespace(value));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

}
