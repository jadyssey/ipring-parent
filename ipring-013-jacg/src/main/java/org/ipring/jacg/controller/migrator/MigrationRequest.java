package org.ipring.jacg.controller.migrator;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import java.util.Objects;

/**
 * 一次代码迁移请求的入参 DTO。
 *
 * <p>所有字段均有默认值，可通过 REST 接口按需覆盖。</p>
 */
public class MigrationRequest {

    /** 入口类的简单类名或全限定名（必填） */
    @NotBlank(message = "入口类名不能为空")
    private String entryClassName;

    /** 入口方法名（必填） */
    @NotBlank(message = "入口方法名不能为空")
    private String entryMethodName;

    /** 源项目根路径（必填） */
    @NotBlank(message = "源项目路径不能为空")
    private String sourceProjectPath;

    /** 目标项目根路径（必填） */
    @NotBlank(message = "目标项目路径不能为空")
    private String destProjectPath;

    /** 调用链展开最大深度（可选，默认 120） */
    @Min(value = 1, message = "最大展开深度必须 >= 1")
    private int maxExpandLevel = 120;

    /** 是否仅预览迁移计划而不实际执行（可选，默认 false） */
    private boolean previewOnly;

    /** GitNexus 仓库名（可选，默认 null 表示自动选择唯一已索引仓库） */
    private String repoName;

    // ======================== getters / setters ========================

    public String getEntryClassName() {
        return entryClassName;
    }

    public void setEntryClassName(String entryClassName) {
        this.entryClassName = entryClassName;
    }

    public String getEntryMethodName() {
        return entryMethodName;
    }

    public void setEntryMethodName(String entryMethodName) {
        this.entryMethodName = entryMethodName;
    }

    public String getSourceProjectPath() {
        return sourceProjectPath;
    }

    public void setSourceProjectPath(String sourceProjectPath) {
        this.sourceProjectPath = sourceProjectPath;
    }

    public String getDestProjectPath() {
        return destProjectPath;
    }

    public void setDestProjectPath(String destProjectPath) {
        this.destProjectPath = destProjectPath;
    }

    public int getMaxExpandLevel() {
        return maxExpandLevel;
    }

    public void setMaxExpandLevel(int maxExpandLevel) {
        this.maxExpandLevel = maxExpandLevel;
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public void setPreviewOnly(boolean previewOnly) {
        this.previewOnly = previewOnly;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MigrationRequest)) return false;
        MigrationRequest that = (MigrationRequest) o;
        return maxExpandLevel == that.maxExpandLevel
                && previewOnly == that.previewOnly
                && Objects.equals(entryClassName, that.entryClassName)
                && Objects.equals(entryMethodName, that.entryMethodName)
                && Objects.equals(sourceProjectPath, that.sourceProjectPath)
                && Objects.equals(destProjectPath, that.destProjectPath)
                && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryClassName, entryMethodName, sourceProjectPath,
                destProjectPath, maxExpandLevel, previewOnly, repoName);
    }

    @Override
    public String toString() {
        return "MigrationRequest{entryClassName='" + entryClassName
                + "', entryMethodName='" + entryMethodName
                + "', source='" + sourceProjectPath
                + "', dest='" + destProjectPath
                + "', maxLevel=" + maxExpandLevel
                + ", previewOnly=" + previewOnly
                + ", repoName='" + repoName + "'}";
    }
}
