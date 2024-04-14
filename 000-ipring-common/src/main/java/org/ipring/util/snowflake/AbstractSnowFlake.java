package org.ipring.util.snowflake;

import org.ipring.enums.subcode.SystemServiceCode;
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
    protected abstract long getNewTime();
    protected abstract long getStartStmp();

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

    private long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastStmp = -1L;//上一次时间戳

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
        if (currStmp < lastStmp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currStmp == lastStmp) {
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

        return (currStmp - getStartStmp()) << timestampLeft() //时间戳部分
                | machineId << machineLeft()             //机器标识部分
                | sequence;                             //序列号部分
    }

    private long getNextMill() {
        long mill = getNewTime();
        // 时钟回拨超过3秒
        if (lastStmp - mill > 3) throw new ServiceException(SystemServiceCode.SystemApi.SNOW_FLAKE);

        while (mill <= lastStmp) {
            mill = getNewTime();
        }
        return mill;
    }
}
