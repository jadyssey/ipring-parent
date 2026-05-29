package org.ipring.jacg.controller.migrator;

/**
 * 迁移流程的各个阶段枚举，用于异常定位和日志追踪。
 */
public enum MigrationStep {

    /** 配置校验 */
    CONFIG_VALIDATION,
    /** GitNexus 知识图谱查询 - 入口解析 */
    ENTRY_RESOLUTION,
    /** GitNexus 调用链展开 */
    CALL_CHAIN_EXPANSION,
    /** GitNexus 依赖收集 */
    DEPENDENCY_COLLECTION,
    /** GitNexus 实现类解析 */
    IMPLEMENTATION_RESOLUTION,
    /** 代码生成 */
    CODE_GENERATION,
    /** Mapper XML 同步 */
    XML_SYNC,
    /** 完成后验证 */
    POST_MIGRATION_VALIDATION
}
