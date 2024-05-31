package org.ipring.util.snowflake;

/**
 * 雪花算法生成账号id
 *
 * @author lgj
 * @date 2024/4/3
 **/
public class AccountSnowFlake extends AbstractSnowFlake {

    public AccountSnowFlake(long machineId) {
        super(machineId);
    }

    private static final long ONE_MINUTES = 60 * 1000;
    @Override
    protected long getTimeUnit() {
        // 每分钟
        return ONE_MINUTES;
    }

    @Override
    protected long getSequenceBit() {
        //序列号占用的位数 每分钟分钟内支持并发512个账号创建
        return 9;
    }

    @Override
    protected long getMachineBit() {
        return 6;
    }
}
