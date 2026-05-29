package org.ipring.jacg.controller.migrator;

/**
 * 代码迁移服务的核心接口。
 *
 * <p>定义了一次迁移任务的所有阶段，每个方法职责单一。
 * 所有阶段间的数据交互均通过 MigrationPlan 传递，保证接口无副作用。</p>
 */
public interface MigrationService {

    /**
     * 执行完整的代码迁移流程。
     *
     * <p>依次执行：配置校验 → 入口解析 → 调用链展开 → 依赖收集 → 代码生成 → Mapper XML 同步。</p>
     *
     * @param request 迁移请求参数
     * @return 迁移执行结果
     */
    MigrationResult migrate(MigrationRequest request);

    /**
     * 仅预览迁移计划，不实际写入任何文件。
     *
     * <p>与 {@link #migrate(MigrationRequest)} 执行完全相同的分析阶段，
     * 但在代码生成阶段不写盘，仅返回计划摘要。</p>
     *
     * @param request 迁移请求参数
     * @return 迁移计划预览结果
     */
    MigrationResult preview(MigrationRequest request);

    /**
     * 验证入口类和方法是否在源项目中存在且可分析。
     *
     * @param request 迁移请求参数（仅需 entryClassName 和 entryMethodName）
     * @return 验证结果
     */
    MigrationResult validate(MigrationRequest request);
}
