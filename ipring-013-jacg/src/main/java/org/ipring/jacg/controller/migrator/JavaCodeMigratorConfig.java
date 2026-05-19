package org.ipring.jacg.controller.migrator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Java 代码迁移工具的静态配置类。
 * <p>
 * 集中管理迁移工具的所有可配置参数：入口类/方法、路径、行为控制常量以及过滤规则。
 * 迁移工具 {@link JavaCodeMigrator} 内部所有需要参数的地方均从此类读取。
 * <p>
 * <b>使用前请修改以下四个入口常量：</b>
 * <ul>
 *   <li>{@link #ENTRY_CLASS_NAME} — 入口类名</li>
 *   <li>{@link #ENTRY_METHOD_NAME} — 入口方法名</li>
 *   <li>{@link #SOURCE_PROJECT_PATH} — 源项目根路径</li>
 *   <li>{@link #DEST_PROJECT_PATH} — 目标项目根路径</li>
 * </ul>
 *
 * @see JavaCodeMigrator
 */
public final class JavaCodeMigratorConfig {

    private JavaCodeMigratorConfig() {
        // 工具类，禁止实例化
    }

    // ======================== 入口配置 ========================

    /**
     * 入口类的简单类名或全限定名。
     * <p>修改此常量为你要迁移的起始 Controller/Service 类名。
     * 支持简单类名（如 {@code "OmsOrderController"}）或全限定名（如 {@code "com.example.controller.OmsOrderController"}）。
     */
    public static final String ENTRY_CLASS_NAME = "OmsOrderController";

    /**
     * 入口方法名（不含参数列表）。
     * <p>修改此常量为你要迁移的起始方法名。
     * 例如：{@code "edit"}、{@code "createOrder"}。
     */
    public static final String ENTRY_METHOD_NAME = "edit";

    /**
     * 源项目的根路径（绝对路径）。
     * <p>工具将从此路径及其所有子模块的 {@code src/main/java} 目录扫描 Java 源文件。
     * 修改此常量为你的源项目根路径。
     */
    public static final String SOURCE_PROJECT_PATH = "D:\\git\\usCode\\dbu-mod-delivery";

    /**
     * 目标项目的根路径（绝对路径）。
     * <p>筛选出的源文件将被复制到 {@code <destProjectPath>/src/main/java/} 下对应的包路径中。
     * 修改此常量为你的目标项目根路径。
     */
    public static final String DEST_PROJECT_PATH = "D:\\git\\usCode\\dbu-mod-waybill\\dbu-mod-waybill-provider";

    // ======================== 行为常量 ========================

    /**
     * Java 源文件的后缀名。
     * <p>用于扫描时过滤非 Java 文件，以及拼接目标文件路径。
     */
    public static final String JAVA_SUFFIX = ".java";

    /**
     * 调用链展开的最大递归层数。
     * <p>超过此层数的方法调用将不再继续展开，防止无限递归或过深展开。
     * 默认值 120 足以覆盖绝大多数业务调用链。
     */
    public static final int MAX_EXPAND_LEVEL = 120;

    /**
     * 源项目中的 Java 源码根路径（相对于项目根路径）。
     */
    public static final String SRC_MAIN_JAVA = "src/main/java";

    // ======================== 过滤规则：JDK 内置包前缀 ========================

    /**
     * JDK 内置包前缀集合。
     * <p>匹配以下前缀的全限定类名将被视为 JDK 类型，不参与迁移：
     * <ul>
     *   <li>{@code java.*} — Java 标准库</li>
     *   <li>{@code javax.*} — Java 扩展库</li>
     *   <li>{@code sun.*} — Oracle/Sun 内部 API</li>
     *   <li>{@code jdk.*} — JDK 内部 API</li>
     *   <li>{@code com.sun.*} — Sun 内部实现</li>
     * </ul>
     */
    public static final Set<String> JDK_PREFIXES = new HashSet<>(Arrays.asList(
            "java.",
            "javax.",
            "sun.",
            "jdk.",
            "com.sun."
    ));

    // ======================== 过滤规则：第三方框架包前缀 ========================

    /**
     * 明确不参与迁移的第三方框架/库包前缀集合。
     * <p>匹配以下前缀的全限定类名将被自动过滤，不会被迁移到目标项目。
     * 因为这些依赖通常由 Maven/Gradle 管理，不需要复制源码。
     * <p>如需迁移特定第三方库的源码，请从本集合中移除对应前缀。
     *
     * <h4>当前排除列表</h4>
     * <ul>
     *   <li>{@code org.springframework.*} — Spring Framework 全系</li>
     *   <li>{@code com.baomidou.*} — MyBatis-Plus</li>
     *   <li>{@code org.mybatis.*} — MyBatis</li>
     *   <li>{@code org.apache.*} — Apache Commons / Maven / Log4j 等</li>
     *   <li>{@code com.alibaba.*} — Druid / FastJSON 等</li>
     *   <li>{@code com.google.*} — Guava / Gson / Protobuf 等</li>
     *   <li>{@code com.fasterxml.*} — Jackson</li>
     *   <li>{@code lombok.*} — Lombok 注解处理</li>
     *   <li>{@code io.swagger.*} — Swagger/OpenAPI</li>
     *   <li>{@code com.github.javaparser} — JavaParser（解析工具自身）</li>
     *   <li>{@code javax.servlet.*} / {@code jakarta.*} — Servlet API</li>
     *   <li>{@code org.slf4j.*} / {@code ch.qos.logback.*} — 日志框架</li>
     *   <li>{@code com.zt.Util} — 项目内公共工具包（不走迁移）</li>
     *   <li>{@code org.ipring.ipring-000-common} — 项目内公共模块（不走迁移）</li>
     * </ul>
     */
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
}
