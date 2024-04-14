package org.ipring.model.dobj.broker;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/02 11:00
 * @description:
 */
@Data
@TableName("t_ac_broker")
public class BrokerDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String logo;
    private Long createTime;
    private Long updateTime;
    private Integer del;
}
