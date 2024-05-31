package org.ipring.util.snowflake;

/**
 * 雪花算法生成分布式序列号
 *
 * @author lgj
 * @date 2024/4/3
 **/
public class OrderSnowFlake extends AbstractSnowFlake {

    public OrderSnowFlake(long machineId) {
        super(machineId);
    }

    @Override
    protected long getTimeUnit() {
        // 每秒
        return 1000;
    }

    @Override
    protected long getSequenceBit() {
        //序列号占用的位数 每秒并发最大512
        return 9;
    }

    @Override
    protected long getMachineBit() {
        //机器标识占用的位数 最大值32
        return 5;
    }
}
