package org.ipring.jacg.controller.migrator;

/**
 * 迁移过程中出现的异常，统一包装 GitNexus 查询失败、代码生成错误等场景。
 */
public class MigrationException extends RuntimeException {

    private final MigrationStep step;

    public MigrationException(MigrationStep step, String message) {
        super(message);
        this.step = step;
    }

    public MigrationException(MigrationStep step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
    }

    public MigrationStep getStep() {
        return step;
    }

    @Override
    public String toString() {
        return "MigrationException{step=" + step + ", message=" + getMessage() + '}';
    }
}
