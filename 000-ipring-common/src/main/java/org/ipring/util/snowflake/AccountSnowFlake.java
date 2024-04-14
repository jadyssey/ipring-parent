package org.ipring.util.snowflake;

/**
 * 雪花算法生成账号id
 *
 * @author lgj
 * @date 2024/4/3
 **/
public class AccountSnowFlake extends AbstractSnowFlake {

    /**
     * 起始的时间戳:2024-04-01 00:00:00，使用时此值不可修改
     */
    private final static long START_STMP = 1711900800L / 60;

    @Override
    protected long getStartStmp() {
        return START_STMP;
    }

    public AccountSnowFlake(long machineId) {
        super(machineId);
    }

    private static final long ONE_MINUTES = 60 * 1000;

    @Override
    protected long getNewTime() {
        // 每分钟
        return System.currentTimeMillis() / ONE_MINUTES;
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
