package org.ipring.jacg.controller.migrator;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ipring.jacg.util.JavaParseLogUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

/**
 * Java 代码迁移 REST 控制器 —— 通过 GitNexus 知识图谱驱动代码迁移。
 *
 * <p>重构后的入口类，所有迁移核心逻辑统一交由 {@link GitNexusClient}（GitNexus MCP）
 * 处理，不再手动解析源码、构建调用链索引或分析依赖关系。</p>
 *
 * <h4>架构说明</h4>
 * <pre>
 *  REST API → JavaCodeMigrator (Controller)
 *                ↓
 *          MigrationService (Interface)
 *                ↓
 *          MigrationServiceImpl
 *           /              \
 *    GitNexusClient    CodeGenerator
 *   (知识图谱查询)      (代码生成写入)
 * </pre>
 *
 * <h4>RESTful 接口</h4>
 * <ul>
 *   <li>{@code POST /api/migration/start} — 启动迁移</li>
 *   <li>{@code POST /api/migration/preview} — 预览迁移计划</li>
 *   <li>{@code POST /api/migration/validate} — 验证入口有效性</li>
 *   <li>{@code GET  /api/migration/health} — 健康检查</li>
 * </ul>
 */
@Api(tags = "Java代码迁移")
@RestController
@RequestMapping("/api/migration")
@Validated
public class JavaCodeMigrator {

    private final MigrationService migrationService;
    private final GitNexusClient gitNexusClient;

    /**
     * 构造迁移控制器，初始化 GitNexus 集成和迁移服务。
     *
     * <p>默认使用 GitNexusMCPClient（JSON-RPC over stdio 实现）。</p>
     */
    public JavaCodeMigrator() {
        this.gitNexusClient = new GitNexusMCPClient();
        this.migrationService = new MigrationServiceImpl(gitNexusClient);
    }

    /**
     * 构造迁移控制器（用于依赖注入或测试）。
     *
     * @param gitNexusClient GitNexus 客户端实现
     */
    public JavaCodeMigrator(GitNexusClient gitNexusClient) {
        this.gitNexusClient = gitNexusClient;
        this.migrationService = new MigrationServiceImpl(gitNexusClient);
    }

    // ======================== RESTful 接口 ========================

    /**
     * 启动一次完整的代码迁移。
     *
     * <p>依次执行：配置校验 → GitNexus 入口解析 → 调用链展开 → 依赖收集 → 代码生成 → Mapper XML 同步。</p>
     *
     * @param request 迁移请求参数（包含入口类、方法、源/目标路径等）
     * @return 迁移执行结果，包含成功/失败状态、统计信息和警告
     */
    @ApiOperation("启动代码迁移")
    @PostMapping("/start")
    public MigrationResult startMigration(@Valid @RequestBody MigrationRequest request) {
        JavaParseLogUtils.logInfo("Migration start requested: " + request);
        try {
            MigrationResult result = migrationService.migrate(request);
            JavaParseLogUtils.logInfo("Migration completed: success=" + result.isSuccess()
                    + ", types=" + result.getTotalTypes()
                    + ", elapsed=" + result.getElapsedMs() + "ms");
            return result;
        } catch (Exception e) {
            JavaParseLogUtils.logWarn("Migration failed: " + e.getMessage());
            MigrationResult errorResult = new MigrationResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("服务异常: " + e.getMessage());
            errorResult.setFailedStep(MigrationStep.CODE_GENERATION);
            return errorResult;
        }
    }

    /**
     * 预览迁移计划，不实际写入任何文件。
     *
     * <p>执行与 {@link #startMigration(MigrationRequest)} 完全相同的分析阶段，
     * 但跳过代码生成步骤，仅返回迁移计划摘要（类型分类、统计信息）。</p>
     *
     * @param request 迁移请求参数
     * @return 迁移计划预览结果
     */
    @ApiOperation("预览迁移计划（不写盘）")
    @PostMapping("/preview")
    public MigrationResult previewMigration(@Valid @RequestBody MigrationRequest request) {
        JavaParseLogUtils.logInfo("Migration preview requested: " + request);
        MigrationRequest previewReq = copyWithPreview(request);
        return migrationService.preview(previewReq);
    }

    /**
     * 验证入口类和方法在源项目中是否可分析。
     *
     * <p>通过 GitNexus 查询知识图谱，确认入口类和方法存在且连接有效的执行流。</p>
     *
     * @param request 迁移请求参数（仅需 entryClassName 和 entryMethodName）
     * @return 验证结果
     */
    @ApiOperation("验证入口有效性")
    @PostMapping("/validate")
    public MigrationResult validateEntry(@Valid @RequestBody MigrationRequest request) {
        JavaParseLogUtils.logInfo("Entry validation requested: "
                + request.getEntryClassName() + "." + request.getEntryMethodName());
        return migrationService.validate(request);
    }

    /**
     * GitNexus 连接健康检查。
     *
     * <p>检查 GitNexus MCP 服务是否可用、是否有已索引的仓库。</p>
     *
     * @return 健康状态
     */
    @ApiOperation("GitNexus 连接健康检查")
    @GetMapping("/health")
    public MigrationResult healthCheck() {
        MigrationResult result = new MigrationResult();
        long start = System.currentTimeMillis();
        try {
            List<GitNexusClient.RepoInfo> repos = gitNexusClient.listRepos();
            result.setSuccess(true);
            result.setTotalTypes(repos.size());
            for (GitNexusClient.RepoInfo repo : repos) {
                result.addWarning(String.format(
                        "%s → files=%d, nodes=%d, edges=%d, communities=%d, processes=%d",
                        repo.getName(), repo.getFiles(), repo.getNodes(),
                        repo.getEdges(), repo.getCommunities(), repo.getProcesses()));
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("GitNexus 连接失败: " + e.getMessage());
            result.setFailedStep(MigrationStep.CONFIG_VALIDATION);
        }
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }

    // ======================== main入口（兼容旧版命令行调用） ========================

    /**
     * 命令行入口。
     *
     * <p>兼容旧版 {@code JavaCodeMigrator.main()} 调用方式。</p>
     *
     * @param args 命令行参数（保留兼容性，当前忽略）
     * @throws Exception 执行异常
     */
    public static void main(String[] args) throws Exception {
        JavaParseLogUtils.logInfo("JavaCodeMigrator starting in standalone mode...");

        // 使用默认配置创建请求
        MigrationRequest request = new MigrationRequest();
        request.setEntryClassName("OmsOrderController");
        request.setEntryMethodName("edit");
        request.setSourceProjectPath("D:\\git\\usCode\\dbu-mod-delivery");
        request.setDestProjectPath("D:\\git\\usCode\\dbu-mod-waybill\\dbu-mod-waybill-provider");

        JavaCodeMigrator migrator = new JavaCodeMigrator();
        try {
            MigrationResult result = migrator.migrationService.migrate(request);
            JavaParseLogUtils.logInfo("========== Migration Complete ==========");
            JavaParseLogUtils.logInfo("  Success    : " + result.isSuccess());
            JavaParseLogUtils.logInfo("  Total types: " + result.getTotalTypes());
            JavaParseLogUtils.logInfo("  Stats      : " + result.getStats().getWholeCopied()
                    + " copied, " + result.getStats().getMethodNewFile()
                    + " reduced, " + result.getStats().getMethodMerged()
                    + " merged, " + result.getStats().getFailed() + " failed");
            JavaParseLogUtils.logInfo("  Elapsed    : " + result.getElapsedMs() + "ms");
            if (!result.isSuccess()) {
                JavaParseLogUtils.logWarn("  ERROR: " + result.getErrorMessage());
            }
        } finally {
            if (migrator.gitNexusClient instanceof GitNexusMCPClient) {
                ((GitNexusMCPClient) migrator.gitNexusClient).shutdown();
            }
        }
    }

    // ======================== 辅助方法 ========================

    private static MigrationRequest copyWithPreview(MigrationRequest source) {
        MigrationRequest copy = new MigrationRequest();
        copy.setEntryClassName(source.getEntryClassName());
        copy.setEntryMethodName(source.getEntryMethodName());
        copy.setSourceProjectPath(source.getSourceProjectPath());
        copy.setDestProjectPath(source.getDestProjectPath());
        copy.setMaxExpandLevel(source.getMaxExpandLevel());
        copy.setPreviewOnly(true);
        copy.setRepoName(source.getRepoName());
        return copy;
    }
}
