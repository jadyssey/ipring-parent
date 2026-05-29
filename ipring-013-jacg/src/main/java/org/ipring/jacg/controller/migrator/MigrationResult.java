package org.ipring.jacg.controller.migrator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 迁移任务的执行结果 DTO。
 */
public class MigrationResult {

    /** 是否成功 */
    private boolean success;

    /** 迁移计划中涉及的类型总数 */
    private int totalTypes;

    /** 各分类的类型统计 */
    private Map<String, List<String>> typesByCategory = new LinkedHashMap<>();

    /** 执行统计 */
    private MigrationStats stats = new MigrationStats();

    /** 警告信息列表 */
    private List<String> warnings = new ArrayList<>();

    /** 错误信息（仅在失败时填充） */
    private String errorMessage;

    /** 异常发生的步骤 */
    private MigrationStep failedStep;

    /** 执行耗时（毫秒） */
    private long elapsedMs;

    /** 执行时间戳 */
    private Instant timestamp = Instant.now();

    // ======================== getters / setters ========================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTotalTypes() {
        return totalTypes;
    }

    public void setTotalTypes(int totalTypes) {
        this.totalTypes = totalTypes;
    }

    public Map<String, List<String>> getTypesByCategory() {
        return typesByCategory;
    }

    public void setTypesByCategory(Map<String, List<String>> typesByCategory) {
        this.typesByCategory = typesByCategory;
    }

    public MigrationStats getStats() {
        return stats;
    }

    public void setStats(MigrationStats stats) {
        this.stats = stats;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public MigrationStep getFailedStep() {
        return failedStep;
    }

    public void setFailedStep(MigrationStep failedStep) {
        this.failedStep = failedStep;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    // ======================== 内部类 ========================

    public static class MigrationStats {
        private int wholeCopied;
        private int wholeSkipped;
        private int methodNewFile;
        private int methodMerged;
        private int callChainSkipped;
        private int xmlSynced;
        private int failed;

        public int getWholeCopied() { return wholeCopied; }
        public void setWholeCopied(int wholeCopied) { this.wholeCopied = wholeCopied; }

        public int getWholeSkipped() { return wholeSkipped; }
        public void setWholeSkipped(int wholeSkipped) { this.wholeSkipped = wholeSkipped; }

        public int getMethodNewFile() { return methodNewFile; }
        public void setMethodNewFile(int methodNewFile) { this.methodNewFile = methodNewFile; }

        public int getMethodMerged() { return methodMerged; }
        public void setMethodMerged(int methodMerged) { this.methodMerged = methodMerged; }

        public int getCallChainSkipped() { return callChainSkipped; }
        public void setCallChainSkipped(int callChainSkipped) { this.callChainSkipped = callChainSkipped; }

        public int getXmlSynced() { return xmlSynced; }
        public void setXmlSynced(int xmlSynced) { this.xmlSynced = xmlSynced; }

        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
    }
}
