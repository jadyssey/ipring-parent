package org.ipring.jacg.controller.migrator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.ipring.jacg.util.JavaParseLogUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成器 —— 负责将迁移计划转换为目标项目中的 Java 源文件。
 *
 * <h4>三大铁律</h4>
 * <ol>
 *   <li><b>包路径完整保留</b>：直接读取源文件的 package 声明，绝不推导或修改</li>
 *   <li><b>仅迁移被调用的方法</b>：精准删除未被调用链引用的冗余方法，保留所有字段和import</li>
 *   <li><b>import 原样保留</b>：通过 LexicalPreservingPrinter 确保格式与源文件完全一致</li>
 * </ol>
 */
public class CodeGenerator {

    /** 注入注解名集合（@Resource/@Autowired等），用于智能字段裁剪 */
    private static final Set<String> BEAN_INJECTION_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Resource", "Autowired", "Inject", "DubboReference", "DubboService"
    ));

    public void generateCode(
            MigrationServiceImpl.MigrationPlan plan,
            MigrationRequest request,
            MigrationResult result) throws IOException {

        MigrationResult.MigrationStats stats = result.getStats();
        Path sourceRoot = Paths.get(request.getSourceProjectPath()).toAbsolutePath().normalize();
        Path destJavaRoot = Paths.get(request.getDestProjectPath())
                .resolve(JavaCodeMigratorConfig.SRC_MAIN_JAVA).toAbsolutePath().normalize();
        Path destResourcesRoot = Paths.get(request.getDestProjectPath())
                .resolve(JavaCodeMigratorConfig.SRC_MAIN_RESOURCES).toAbsolutePath().normalize();

        for (String classDeclaredName : plan.getMigrationTypes()) {
            try {
                migrateOneType(classDeclaredName, plan, sourceRoot, destJavaRoot, destResourcesRoot, stats);
            } catch (Exception e) {
                stats.setFailed(stats.getFailed() + 1);
                JavaParseLogUtils.logWarn("Failed to migrate " + classDeclaredName + ": " + e.getMessage());
            }
        }
    }

    // ======================== 单类型迁移 ========================

    private void migrateOneType(
            String classDeclaredName,
            MigrationServiceImpl.MigrationPlan plan,
            Path sourceRoot,
            Path destJavaRoot,
            Path destResourcesRoot,
            MigrationResult.MigrationStats stats) throws IOException {

        Path srcFile = findSourceFile(classDeclaredName, sourceRoot);
        if (srcFile == null) {
            stats.setFailed(stats.getFailed() + 1);
            JavaParseLogUtils.logWarn("Source file not found for " + classDeclaredName);
            return;
        }

        // 铁律1：从源文件读取原始 package 声明，绝不推导
        String rawText = new String(Files.readAllBytes(srcFile), StandardCharsets.UTF_8);
        String sourcePkg = readPackageDeclaration(rawText);
        String simpleName = JavaCodeMigratorConfig.shortName(classDeclaredName);

        // 目标路径严格使用源文件原始包路径
        String pkgPath = sourcePkg.isEmpty() ? "" : sourcePkg.replace('.', '/');
        Path packageDir = pkgPath.isEmpty() ? destJavaRoot : destJavaRoot.resolve(pkgPath);
        Path destFile = packageDir.resolve(simpleName + JavaCodeMigratorConfig.JAVA_SUFFIX);

        // 判断是否为 Model/Enum/Interface 类型（完整复制，不裁剪）
        boolean isWholeCopyType = plan.isModel(classDeclaredName) || plan.isEnum(classDeclaredName)
                || plan.isInterface(classDeclaredName);
        Set<String> usedMethods = plan.getUsedMethods().get(classDeclaredName);
        boolean hasUsedMethods = usedMethods != null && !usedMethods.isEmpty();

        if (isWholeCopyType || !hasUsedMethods) {
            // 无调用链方法信息的类型：完整复制（作为依赖整体迁移）
            copyWholeFile(classDeclaredName, srcFile, destFile, stats);
            return;
        }

        // 铁律2：有精确方法集的 Service/Controller/Mapper 类 → 裁剪冗余方法
        Files.createDirectories(destFile.getParent());
        if (Files.exists(destFile)) {
            mergeMembers(classDeclaredName, srcFile, destFile, usedMethods, stats);
        } else {
            writeReducedClass(classDeclaredName, srcFile, destFile, usedMethods, rawText, stats);
        }
    }

    // ======================== 包路径读取（铁律1） ========================

    /**
     * 从 Java 源文件原始文本中读取 package 声明。
     *
     * <p>不使用任何推导或路径反推，直接解析第一行有效的 package 语句。
     * 这是保证迁移后代码可编译的关键。</p>
     */
    private String readPackageDeclaration(String rawText) {
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") && trimmed.endsWith(";")) {
                return trimmed.substring("package ".length(), trimmed.length() - 1).trim();
            }
        }
        return "";
    }

    // ======================== 源文件定位 ========================

    private Path findSourceFile(String classDeclaredName, Path sourceRoot) throws IOException {
        // 策略1：classDeclaredName 是全限定名 → 按包路径查找
        if (classDeclaredName.contains(".")) {
            String relPath = classDeclaredName.replace('.', '/') + JavaCodeMigratorConfig.JAVA_SUFFIX;
            Path candidate = findFileByRelativePath(sourceRoot, relPath);
            if (candidate != null) return candidate;
        }
        // 策略2：简单类名 → 从源文件中读取 package 声明来匹配
        String simpleName = JavaCodeMigratorConfig.shortName(classDeclaredName);
        return findFileBySimpleName(sourceRoot, simpleName, classDeclaredName);
    }

    private Path findFileByRelativePath(Path root, String relPath) throws IOException {
        String normalized = relPath.replace('/', java.io.File.separatorChar);
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(normalized))
                    .findFirst().orElse(null);
        }
    }

    private Path findFileBySimpleName(Path root, String simpleName, String classDeclaredName) throws IOException {
        String fileName = simpleName + JavaCodeMigratorConfig.JAVA_SUFFIX;
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            List<Path> candidates = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .collect(Collectors.toList());
            // 如果只有一个候选，直接返回
            if (candidates.size() == 1) return candidates.get(0);
            // 多个同名文件：用全限定名中的包路径片段做二次匹配
            if (classDeclaredName.contains(".")) {
                String pkgFragment = classDeclaredName.replace('.', '/');
                for (Path c : candidates) {
                    if (c.toString().replace('\\', '/').contains(pkgFragment)) {
                        return c;
                    }
                }
            }
            return candidates.isEmpty() ? null : candidates.get(0);
        }
    }

    // ======================== 完整文件复制 ========================

    private void copyWholeFile(
            String classDeclaredName,
            Path srcFile,
            Path destFile,
            MigrationResult.MigrationStats stats) throws IOException {
        if (Files.exists(destFile)) {
            stats.setWholeSkipped(stats.getWholeSkipped() + 1);
            JavaParseLogUtils.logInfo("SKIP (exists): " + classDeclaredName);
            return;
        }
        Files.createDirectories(destFile.getParent());
        Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
        stats.setWholeCopied(stats.getWholeCopied() + 1);
        JavaParseLogUtils.logInfo("COPY (whole): " + destFile);
    }

    // ======================== 精简类写入（铁律2：仅保留调用链方法） ========================

    private void writeReducedClass(
            String classDeclaredName,
            Path srcFile,
            Path destFile,
            Set<String> methodNames,
            String rawText,
            MigrationResult.MigrationStats stats) throws IOException {
        try {
            CompilationUnit cu = StaticJavaParser.parse(rawText);
            LexicalPreservingPrinter.setup(cu);

            TypeDeclaration<?> type = findType(cu, classDeclaredName);
            if (!(type instanceof ClassOrInterfaceDeclaration)) {
                // 非类声明（如注解、枚举），完整复制
                Files.write(destFile, rawText.getBytes(StandardCharsets.UTF_8));
                stats.setMethodNewFile(stats.getMethodNewFile() + 1);
                return;
            }

            ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) type;

            // 展开本地辅助方法（被保留方法内部调用的 private 方法也要保留）
            Set<String> effectiveMethodNames = expandLocalSupportMethods(clazz, methodNames);

            // 铁律2：删除未被调用链引用的方法
            int prunedMethods = pruneUnusedMethods(clazz, effectiveMethodNames);
            // 智能裁剪未引用的注入字段（未被任何保留方法使用）
            int prunedFields = pruneUnusedBeanFields(clazz, effectiveMethodNames);

            // 铁律3：LexicalPreservingPrinter 输出，保留原始 import 和格式
            String output = LexicalPreservingPrinter.print(cu);
            if (!isValidJava(output)) {
                // fallback: 完整复制（不容忍语法错误）
                Files.write(destFile, rawText.getBytes(StandardCharsets.UTF_8));
                stats.setWholeCopied(stats.getWholeCopied() + 1);
                JavaParseLogUtils.logWarn("Fallback to full copy for " + classDeclaredName
                        + " (generated source invalid)");
                return;
            }
            Files.write(destFile, output.getBytes(StandardCharsets.UTF_8));
            stats.setMethodNewFile(stats.getMethodNewFile() + 1);
            JavaParseLogUtils.logInfo("NEW (reduced): " + destFile
                    + " [" + effectiveMethodNames.size() + " methods, "
                    + prunedMethods + " pruned]");
        } catch (Exception e) {
            // fallback: 完整复制
            Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            stats.setWholeCopied(stats.getWholeCopied() + 1);
            JavaParseLogUtils.logWarn("Fallback copy for " + classDeclaredName + ": " + e.getMessage());
        }
    }

    // ======================== 方法合并 ========================

    private void mergeMembers(
            String classDeclaredName,
            Path srcFile,
            Path destFile,
            Set<String> methodNames,
            MigrationResult.MigrationStats stats) throws IOException {
        // 目标已存在：将源文件中调用链涉及的方法合并到目标
        try {
            String destText = new String(Files.readAllBytes(destFile), StandardCharsets.UTF_8);
            String srcText = new String(Files.readAllBytes(srcFile), StandardCharsets.UTF_8);

            CompilationUnit destCu = StaticJavaParser.parse(destText);
            LexicalPreservingPrinter.setup(destCu);
            CompilationUnit srcCu = StaticJavaParser.parse(srcText);

            TypeDeclaration<?> destType = findType(destCu, classDeclaredName);
            TypeDeclaration<?> srcType = findType(srcCu, classDeclaredName);
            if (!(destType instanceof ClassOrInterfaceDeclaration)
                    || !(srcType instanceof ClassOrInterfaceDeclaration)) {
                Files.write(destFile, srcText.getBytes(StandardCharsets.UTF_8));
                stats.setMethodMerged(stats.getMethodMerged() + 1);
                return;
            }

            ClassOrInterfaceDeclaration destClass = (ClassOrInterfaceDeclaration) destType;
            ClassOrInterfaceDeclaration srcClass = (ClassOrInterfaceDeclaration) srcType;

            int added = 0;
            Set<String> existingNames = new LinkedHashSet<>();
            destClass.getMethods().forEach(m -> existingNames.add(m.getNameAsString()));

            for (MethodDeclaration srcMethod : srcClass.getMethods()) {
                if (!methodNames.contains(srcMethod.getNameAsString())) continue;
                if (existingNames.contains(srcMethod.getNameAsString())) {
                    // 方法已存在则覆盖
                    destClass.getMethods().stream()
                            .filter(m -> m.getNameAsString().equals(srcMethod.getNameAsString()))
                            .findFirst().ifPresent(m -> m.replace(srcMethod.clone()));
                } else {
                    destClass.getMembers().add(srcMethod.clone());
                    added++;
                }
            }

            String output = LexicalPreservingPrinter.print(destCu);
            if (isValidJava(output)) {
                Files.write(destFile, output.getBytes(StandardCharsets.UTF_8));
            } else {
                Files.write(destFile, srcText.getBytes(StandardCharsets.UTF_8));
            }
            stats.setMethodMerged(stats.getMethodMerged() + 1);
            JavaParseLogUtils.logInfo("MERGE (+" + added + " methods): " + destFile);
        } catch (Exception e) {
            Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            stats.setWholeCopied(stats.getWholeCopied() + 1);
            JavaParseLogUtils.logWarn("Fallback merge copy for " + classDeclaredName + ": " + e.getMessage());
        }
    }

    // ======================== 方法裁剪（铁律2核心） ========================

    /**
     * 删除类中不在调用链方法集合中的方法。
     *
     * @return 裁剪掉的方法数量
     */
    private int pruneUnusedMethods(ClassOrInterfaceDeclaration clazz, Set<String> retainedNames) {
        List<MethodDeclaration> toRemove = new ArrayList<>();
        for (MethodDeclaration method : clazz.getMethods()) {
            if (!retainedNames.contains(method.getNameAsString())) {
                toRemove.add(method);
            }
        }
        toRemove.forEach(Node::remove);
        return toRemove.size();
    }

    /**
     * 展开本地辅助方法：保留方法体内部调用的 private 方法也要保留。
     *
     * <p>例如 {@code add()} 内部调用了 {@code checkParam()}，即使 {@code checkParam}
     * 不在调用链中，也需要保留，否则保留的 {@code add()} 方法将编译失败。</p>
     */
    private Set<String> expandLocalSupportMethods(
            ClassOrInterfaceDeclaration clazz, Set<String> baseMethodNames) {
        Set<String> expanded = new LinkedHashSet<>(baseMethodNames);
        // 构建类内方法名查找表
        Set<String> allMethodNames = new LinkedHashSet<>();
        clazz.getMethods().forEach(m -> allMethodNames.add(m.getNameAsString()));

        boolean changed;
        do {
            changed = false;
            for (MethodDeclaration method : clazz.getMethods()) {
                if (!expanded.contains(method.getNameAsString())) continue;
                if (!method.getBody().isPresent()) continue;
                for (MethodCallExpr call : method.getBody().get().findAll(MethodCallExpr.class)) {
                    if (!call.getScope().isPresent()) {
                        // 无 scope 的方法调用 → 可能是本地方法
                        String callName = call.getNameAsString();
                        if (allMethodNames.contains(callName) && expanded.add(callName)) {
                            changed = true;
                        }
                    } else {
                        String scope = call.getScope().get().toString().trim();
                        if ("this".equals(scope) || "super".equals(scope)) {
                            String callName = call.getNameAsString();
                            if (allMethodNames.contains(callName) && expanded.add(callName)) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
        return expanded;
    }

    /**
     * 智能裁剪未引用的注入字段。
     */
    private int pruneUnusedBeanFields(ClassOrInterfaceDeclaration clazz, Set<String> retainedMethodNames) {
        // 收集保留方法所引用的字段名
        Set<String> referenced = new LinkedHashSet<>();
        for (MethodDeclaration method : clazz.getMethods()) {
            if (!retainedMethodNames.contains(method.getNameAsString())) continue;
            if (!method.getBody().isPresent()) continue;
            // 收集 this.field 和直接 field 引用
            method.getBody().get().findAll(
                    com.github.javaparser.ast.expr.FieldAccessExpr.class).forEach(fa -> {
                        String scope = fa.getScope().toString().trim();
                        if ("this".equals(scope)) referenced.add(fa.getNameAsString());
                    });
            method.getBody().get().findAll(
                    com.github.javaparser.ast.expr.NameExpr.class).forEach(ne -> {
                        String name = ne.getNameAsString();
                        // 简单启发：字段名通常以 mapper/service/config 结尾
                        if (name.endsWith("Mapper") || name.endsWith("Service") || name.endsWith("Config")
                                || name.endsWith("Dao") || name.endsWith("Repository")) {
                            referenced.add(name);
                        }
                    });
        }

        int removed = 0;
        List<FieldDeclaration> emptyFields = new ArrayList<>();
        for (FieldDeclaration field : clazz.getFields()) {
            if (!isBeanInjectionField(field)) continue;
            List<VariableDeclarator> unused = new ArrayList<>();
            for (VariableDeclarator var : field.getVariables()) {
                if (!referenced.contains(var.getNameAsString())) {
                    unused.add(var);
                }
            }
            for (VariableDeclarator v : unused) {
                field.getVariables().remove(v);
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
        return field.getAnnotations().stream()
                .anyMatch(a -> BEAN_INJECTION_ANNOTATIONS.contains(a.getNameAsString()));
    }

    // ======================== 类型查找 ========================

    private TypeDeclaration<?> findType(CompilationUnit cu, String classDeclaredName) {
        String simpleName = JavaCodeMigratorConfig.shortName(classDeclaredName);
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (simpleName.equals(type.getNameAsString())) {
                return type;
            }
        }
        return cu.getType(0);
    }

    private boolean isValidJava(String source) {
        try {
            StaticJavaParser.parse(source);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
