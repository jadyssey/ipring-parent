package org.ipring.util.snowflake;

/**
 * 雪花算法生成分布式序列号
 *
 * @author lgj
 * @date 2024/4/3
 **/
public class OrderSnowFlake extends AbstractSnowFlake {
    /**
     * 起始的时间戳:2024-04-01 00:00:00，使用时此值不可修改
     */
    private final static long START_STMP = 1711900800L;

    @Override
    protected long getStartStmp() {
        return START_STMP;
    }

    public OrderSnowFlake(long machineId) {
        super(machineId);
    }

    @Override
    protected long getNewTime() {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    protected long getSequenceBit() {
        //序列号占用的位数 每秒并发最大256
        return 8;
    }

    @Override
    protected long getMachineBit() {
        //机器标识占用的位数 最大值64
        return 6;
    }
}
