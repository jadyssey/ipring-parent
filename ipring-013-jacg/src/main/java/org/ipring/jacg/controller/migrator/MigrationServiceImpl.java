package org.ipring.jacg.controller.migrator;

import org.ipring.jacg.util.JavaParseLogUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MigrationService 的实现类，用 GitNexus 知识图谱驱动所有迁移分析逻辑。
 *
 * <p>核心流程：
 * <ol>
 *   <li>通过 GitNexus 查询入口类的执行流，获取调用链中的所有类与方法</li>
 *   <li>通过 GitNexus 递归分析每个类的依赖关系和接口实现</li>
 *   <li>收集完整的迁移类型清单（Model/Enum/Interface/Mapper/普通类）</li>
 *   <li>调用 CodeGenerator 逐类型生成或合并目标代码</li>
 * </ol>
 */
public class MigrationServiceImpl implements MigrationService {

    private final GitNexusClient gitNexus;
    private final CodeGenerator codeGenerator;

    public MigrationServiceImpl(GitNexusClient gitNexusClient) {
        this.gitNexus = gitNexusClient;
        this.codeGenerator = new CodeGenerator();
    }

    public MigrationServiceImpl(GitNexusClient gitNexusClient, CodeGenerator codeGenerator) {
        this.gitNexus = gitNexusClient;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public MigrationResult migrate(MigrationRequest request) {
        return executeMigration(request, false);
    }

    @Override
    public MigrationResult preview(MigrationRequest request) {
        return executeMigration(request, true);
    }

    @Override
    public MigrationResult validate(MigrationRequest request) {
        MigrationResult result = new MigrationResult();
        long start = System.currentTimeMillis();
        try {
            validateRequest(request);
            // 与 resolveEntry 一致：使用 context 验证入口
            GitNexusClient.GitNexusSymbolContext ctx = gitNexus.getSymbolContext(
                    request.getEntryClassName(), "Class", null, request.getRepoName());
            if (ctx.getName() == null || ctx.getName().isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("入口类 " + request.getEntryClassName() + " 在 GitNexus 知识图谱中未找到");
                result.setFailedStep(MigrationStep.ENTRY_RESOLUTION);
            } else {
                result.setSuccess(true);
                result.setTotalTypes(ctx.getMethods() != null ? ctx.getMethods().size() : 0);
                result.addWarning("入口验证通过: " + ctx.getName()
                        + " (" + (ctx.getMethods() != null ? ctx.getMethods().size() : 0) + " methods)");
            }
        } catch (MigrationException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setFailedStep(e.getStep());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("验证异常: " + e.getMessage());
        }
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }

    // ---- 核心迁移流程 ----

    private MigrationResult executeMigration(MigrationRequest request, boolean previewOnly) {
        MigrationResult result = new MigrationResult();
        MigrationPlan plan = new MigrationPlan();
        long start = System.currentTimeMillis();

        try {
            // 1. 配置校验
            validateRequest(request);

            // 2. 通过 GitNexus 解析入口类
            List<GitNexusClient.GitNexusSymbol> entrySymbols = resolveEntry(request, result);
            if (entrySymbols.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("未找到入口类/方法的执行流");
                result.setFailedStep(MigrationStep.ENTRY_RESOLUTION);
                result.setElapsedMs(System.currentTimeMillis() - start);
                return result;
            }

            // 3. 展开调用链 - 所有阶段通过 GitNexus
            expandCallChainViaGitNexus(request, entrySymbols, plan);

            // 4. 收集传递依赖 - 通过 GitNexus context
            collectTransitiveDependenciesViaGitNexus(plan, request.getRepoName());

            // 5. 构建分类视图
            buildCategoryView(plan, result);

            JavaParseLogUtils.logInfo("Migration plan: " + plan.getMigrationTypes().size() + " types to migrate.");

            if (!previewOnly) {
                // 6. 代码生成
                codeGenerator.generateCode(plan, request, result);
            }

            result.setSuccess(true);
        } catch (MigrationException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setFailedStep(e.getStep());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("迁移异常: " + e.getMessage());
            result.setFailedStep(MigrationStep.CODE_GENERATION);
        }

        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }

    // ---- 步骤实现 ----

    private void validateRequest(MigrationRequest request) {
        if (request.getEntryClassName() == null || request.getEntryClassName().trim().isEmpty()) {
            throw new MigrationException(MigrationStep.CONFIG_VALIDATION, "入口类名不能为空");
        }
        if (request.getEntryMethodName() == null || request.getEntryMethodName().trim().isEmpty()) {
            throw new MigrationException(MigrationStep.CONFIG_VALIDATION, "入口方法名不能为空");
        }
        Path sourceRoot = Paths.get(request.getSourceProjectPath()).toAbsolutePath().normalize();
        if (!java.nio.file.Files.isDirectory(sourceRoot)) {
            throw new MigrationException(MigrationStep.CONFIG_VALIDATION, "源项目路径不存在: " + sourceRoot);
        }
    }

    private List<GitNexusClient.GitNexusSymbol> resolveEntry(MigrationRequest request, MigrationResult result) {
        JavaParseLogUtils.logInfo("Resolving entry via GitNexus context: "
                + request.getEntryClassName() + "." + request.getEntryMethodName());

        // 优先使用 context（不需要 FTS 索引），query 作为辅助（需要 FTS）
        GitNexusClient.GitNexusSymbolContext ctx = gitNexus.getSymbolContext(
                request.getEntryClassName(), "Class", null, request.getRepoName());

        if (ctx.getName() == null || ctx.getName().isEmpty()) {
            result.addWarning("GitNexus context 未找到入口类，尝试 query 查询...");
            List<GitNexusClient.GitNexusSymbol> queryResult = gitNexus.queryExecutionFlows(
                    request.getEntryClassName(), request.getEntryMethodName(), request.getRepoName());
            if (queryResult.isEmpty()) {
                throw new MigrationException(MigrationStep.ENTRY_RESOLUTION,
                        "入口类 " + request.getEntryClassName() + " 在 GitNexus 知识图谱中未找到。"
                                + "请确认类名正确且仓库已索引。");
            }
            return queryResult;
        }

        // 构造符号信息
        GitNexusClient.GitNexusSymbol entrySymbol = new GitNexusClient.GitNexusSymbol();
        entrySymbol.setName(ctx.getName());
        entrySymbol.setFilePath(ctx.getFilePath());
        entrySymbol.setKind(ctx.getKind());
        // 从 filePath 推断全限定名
        entrySymbol.setFullName(deriveFullName(ctx));

        List<GitNexusClient.GitNexusSymbol> symbols = new ArrayList<>();
        symbols.add(entrySymbol);
        result.addWarning("入口类已定位: " + entrySymbol.getFullName()
                + " (" + ctx.getMethods().size() + " methods)");
        return symbols;
    }

    private void expandCallChainViaGitNexus(
            MigrationRequest request,
            List<GitNexusClient.GitNexusSymbol> entrySymbols,
            MigrationPlan plan) {

        JavaParseLogUtils.logInfo("Expanding call chain via GitNexus impact analysis...");

        for (GitNexusClient.GitNexusSymbol entry : entrySymbols) {
            String targetName = entry.getFullName().isEmpty() ? entry.getName() : entry.getFullName();
            plan.addMigrationType(targetName);

            // 只记录入口方法名（后续通过 expandLocalSupportMethods 自动保留其内部调用的本地方法）
            plan.recordUsedMethod(targetName, request.getEntryMethodName());
            JavaParseLogUtils.logInfo("Entry method recorded: " + targetName + "#" + request.getEntryMethodName());

            // 通过 GitNexus impact 分析向下展开依赖（不需要 FTS 索引）
            try {
                GitNexusClient.GitNexusImpactResult impact = gitNexus.analyzeImpact(
                        entry.getName(), "downstream", request.getMaxExpandLevel(), request.getRepoName());

                Map<Integer, List<String>> byDepth = impact.getByDepth();
                int totalDep = 0;
                for (Map.Entry<Integer, List<String>> depthEntry : byDepth.entrySet()) {
                    for (String symbol : depthEntry.getValue()) {
                        // impact 返回 File 级节点名称如 "ProductConfigDomainService.java"
                        // 去掉 .java 后缀，并过滤 JDK/第三方
                        String className = stripJavaSuffix(symbol);
                        if (className.isEmpty() || JavaCodeMigratorConfig.isJdkOrThirdPartyType(className)) {
                            continue;
                        }
                        plan.addMigrationType(className);
                        totalDep++;
                    }
                }
                JavaParseLogUtils.logInfo("Impact analysis found " + totalDep
                        + " downstream dependencies across " + byDepth.size() + " levels");
            } catch (Exception e) {
                plan.addWarning("Impact analysis failed for " + entry.getName()
                        + ": " + e.getMessage() + " — proceeding with context-based dependencies only");
            }
        }
    }

    private static String deriveFullName(GitNexusClient.GitNexusSymbolContext ctx) {
        // 从 filePath 推导全限定类名: src/main/java/com/xxx/Controller.java → com.xxx.Controller
        String path = ctx.getFilePath();
        if (path == null || path.isEmpty()) return ctx.getName();
        // 找到 src/main/java 后的部分
        int idx = path.indexOf("src/main/java/");
        if (idx >= 0) {
            String after = path.substring(idx + "src/main/java/".length());
            after = after.replace('/', '.').replace('\\', '.');
            if (after.endsWith(".java")) after = after.substring(0, after.length() - 5);
            return after;
        }
        return ctx.getName();
    }

    private static String stripJavaSuffix(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.endsWith(".java") ? name.substring(0, name.length() - 5) : name;
    }

    private void collectTransitiveDependenciesViaGitNexus(MigrationPlan plan, String repoName) {
        JavaParseLogUtils.logInfo("Collecting transitive dependencies via GitNexus context queries...");

        int previousSize;
        do {
            previousSize = plan.getMigrationTypes().size();
            List<String> snapshot = new ArrayList<>(plan.getMigrationTypes());
            for (String type : snapshot) {
                if (plan.isAnalyzed(type)) continue;

                try {
                    GitNexusClient.GitNexusSymbolContext ctx = gitNexus.getSymbolContext(
                            type, null, null, repoName);

                    // 添加实现
                    if (ctx.getImplementations() != null) {
                        for (String impl : ctx.getImplementations()) {
                            if (!JavaCodeMigratorConfig.isJdkOrThirdPartyType(impl)) {
                                plan.addMigrationType(impl);
                            }
                        }
                    }
                    // 添加接口
                    if (ctx.getInterfaces() != null) {
                        for (String iface : ctx.getInterfaces()) {
                            if (!JavaCodeMigratorConfig.isJdkOrThirdPartyType(iface)) {
                                plan.addMigrationType(iface);
                            }
                        }
                    }
                    // 添加被调用者
                    if (ctx.getCallees() != null) {
                        for (String callee : ctx.getCallees()) {
                            if (!JavaCodeMigratorConfig.isJdkOrThirdPartyType(callee)) {
                                plan.addMigrationType(callee);
                            }
                        }
                    }

                    plan.markAnalyzed(type);
                } catch (Exception e) {
                    plan.addWarning("无法获取 " + type + " 的上下文: " + e.getMessage());
                    plan.markAnalyzed(type);
                }
            }
        } while (plan.getMigrationTypes().size() > previousSize);
    }

    private void buildCategoryView(MigrationPlan plan, MigrationResult result) {
        Map<String, List<String>> categories = new LinkedHashMap<>();

        for (String type : plan.getMigrationTypes()) {
            String category = plan.isModel(type) ? "MODEL"
                    : plan.isEnum(type) ? "ENUM"
                    : plan.isInterface(type) ? "INTERFACE"
                    : plan.isMapper(type) ? "MAPPER"
                    : "CALL_CHAIN";
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(type);
        }

        result.setTypesByCategory(categories);
        result.setTotalTypes(plan.getMigrationTypes().size());
        result.getStats().setCallChainSkipped(plan.getWarnings().size());
    }

    // ======================== 内部迁移计划 ----

    static final class MigrationPlan {
        private final Set<String> migrationTypes = new LinkedHashSet<>();
        private final Set<String> analyzedTypes = new LinkedHashSet<>();
        private final Map<String, Set<String>> usedMethods = new LinkedHashMap<>();
        private final List<String> warnings = new ArrayList<>();

        void addMigrationType(String fullClassName) {
            if (fullClassName != null && !fullClassName.isEmpty()
                    && !JavaCodeMigratorConfig.isJdkOrThirdPartyType(fullClassName)) {
                migrationTypes.add(fullClassName);
            }
        }

        void recordUsedMethod(String className, String methodName) {
            usedMethods.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(methodName);
        }

        void markAnalyzed(String type) { analyzedTypes.add(type); }

        boolean isAnalyzed(String type) { return analyzedTypes.contains(type); }

        void addWarning(String warning) { warnings.add(warning); }

        Set<String> getMigrationTypes() { return migrationTypes; }

        List<String> getWarnings() { return warnings; }

        Map<String, Set<String>> getUsedMethods() { return usedMethods; }

        boolean isModel(String fullClassName) {
            String simpleName = JavaCodeMigratorConfig.shortName(fullClassName);
            for (String suffix : JavaCodeMigratorConfig.MODEL_SUFFIXES) {
                if (simpleName.endsWith(suffix)) return true;
            }
            return false;
        }

        boolean isEnum(String fullClassName) {
            return fullClassName.contains(".enum.") || fullClassName.contains(".enums.");
        }

        boolean isInterface(String fullClassName) {
            // 通过 GitNexus context 判断（此处用启发式规则作为 fallback）
            return false;
        }

        boolean isMapper(String fullClassName) {
            String simpleName = JavaCodeMigratorConfig.shortName(fullClassName);
            return simpleName.endsWith("Mapper");
        }
    }
}
