package org.ipring.jacg.controller.migrator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Java 代码迁移工具的静态配置类。
 *
 * <p>集中管理迁移工具的过滤规则和行为常量。
 * 入口参数（类名、方法名、路径）现在通过 {@link MigrationRequest} 的 REST 接口传入，
 * 不再硬编码在此处。</p>
 *
 * <p>与 GitNexus 集成后，分析逻辑交由 GitNexus 知识图谱处理，
 * 此配置仅保留 JDK/第三方包过滤等静态规则。</p>
 */
public final class JavaCodeMigratorConfig {

    private JavaCodeMigratorConfig() {
        // 工具类，禁止实例化
    }

    // ======================== 行为常量 ========================

    /** Java 源文件的后缀名 */
    public static final String JAVA_SUFFIX = ".java";

    /** 调用链展开的最大递归层数（默认） */
    public static final int DEFAULT_MAX_EXPAND_LEVEL = 120;

    /** Java 源码根路径（相对于项目根路径） */
    public static final String SRC_MAIN_JAVA = "src/main/java";

    /** 资源文件根路径（相对于项目根路径） */
    public static final String SRC_MAIN_RESOURCES = "src/main/resources";

    // ======================== 类型分类后缀 ========================

    /** Model/值对象类的常见后缀 */
    public static final Set<String> MODEL_SUFFIXES = new HashSet<>(Arrays.asList(
            "DO", "DTO", "VO", "PO", "BO", "POJO", "Entity", "Model",
            "Req", "Resp", "Request", "Response", "Param", "Params",
            "Query", "Cmd", "Command", "Event", "Result", "Context"
    ));

    // ======================== 过滤规则：JDK 内置包前缀 ========================

    public static final Set<String> JDK_PREFIXES = new HashSet<>(Arrays.asList(
            "java.", "javax.", "sun.", "jdk.", "com.sun."
    ));

    // ======================== 过滤规则：第三方框架包前缀 ========================

    public static final Set<String> THIRD_PARTY_PREFIXES = new HashSet<>(Arrays.asList(
            "org.springframework.",
            "com.baomidou.",
            "org.mybatis.",
            "org.apache.",
            "com.alibaba.",
            "com.google.",
            "com.fasterxml.",
            "lombok.",
            "io.swagger.",
            "com.github.javaparser",
            "javax.servlet.",
            "jakarta.",
            "org.slf4j.",
            "ch.qos.logback.",
            "com.zt.Util",
            "org.ipring.ipring-000-common"
    ));

    // ======================== 工具方法 ========================

    /**
     * 判断全限定类名是否属于 JDK 或第三方库，不应参与迁移。
     */
    public static boolean isJdkOrThirdPartyType(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) return true;
        for (String prefix : JDK_PREFIXES) {
            if (fullClassName.startsWith(prefix)) return true;
        }
        for (String prefix : THIRD_PARTY_PREFIXES) {
            if (fullClassName.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * 获取全限定类名的简单类名部分。
     */
    public static String shortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return fullName;
        int idx = fullName.lastIndexOf('.');
        return idx < 0 ? fullName : fullName.substring(idx + 1);
    }
}
