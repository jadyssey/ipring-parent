package org.ipring.util.snowflake;

import org.ipring.exception.ServiceException;

/**
 * @author lgj
 * @date 2024/4/12
 **/
public abstract class AbstractSnowFlake {

    /**
     * 序列号和机器标识的位数由子类实现
     */
    protected abstract long getSequenceBit();

    protected abstract long getMachineBit();

    protected abstract long getTimeUnit();

    protected long maxSequence() {
        return ~(-1L << getSequenceBit());
    }

    protected long maxMachineNum() {
        return ~(-1L << getMachineBit());
    }

    protected long machineLeft() {
        return getSequenceBit();
    }

    protected long timestampLeft() {
        return getSequenceBit() + getMachineBit();
    }

    private final long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastStmp = -1L;//上一次时间戳

    /**
     * 起始的时间戳:2024-04-01 00:00:00，使用时此值不可修改
     */
    private final static long START_STMP = 1711900800000L;

    public AbstractSnowFlake(long machineId) {
        if (machineId > maxMachineNum() || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     * 为了防止时钟回拨造成获取id时间过长的问题，请尽可能的提前获取雪花算法id，以防造成业务校验与业务执行时间偏差
     */
    public synchronized long nextId() {
        long currStmp = getNewTime();
        // 回拨超过3秒
        if (currStmp < lastStmp) {
            if (lastStmp - currStmp > 3 * 1000) throw new ServiceException("雪花算法异常");
            while ((currStmp = getNewTime()) < lastStmp) {
            }
        }

        long lastTimeUnit = lastStmp / getTimeUnit();
        long currStmpUnit = currStmp / getTimeUnit();

        if (currStmpUnit == lastTimeUnit) {
            //相同秒内，序列号自增
            sequence = (sequence + 1) & maxSequence();
            //同一秒的序列数已经达到最大
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }
        lastStmp = currStmp;

        return ((currStmp - START_STMP) / getTimeUnit()) << timestampLeft() //时间戳部分
                | machineId << machineLeft()             //机器标识部分
                | sequence;                             //序列号部分
    }

    public long getNewTime() {
        return System.currentTimeMillis();
    }

    private long getNextMill() {
        long mill = getNewTime();
        long jugeTime = lastStmp / getTimeUnit();
        while (mill / getTimeUnit() <= jugeTime) {
            mill = getNewTime();
        }
        return mill;
    }
}
