package org.ipring.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author lgj
 * @date 2024/4/16
 **/
@Data
public class OrderUniqEntity {
    @ApiModelProperty("交易账号")
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("订单号")
    private Long orderId;

    /**
     * 获取当前订单唯一键
     *
     * @return
     */
    @JsonIgnore
    public String getOrderUniq() {
        return getOrderByUniq(this.getAccountId(), this.getOrderId());
    }

    public static String getOrderByUniq(Long accountId, Long orderId) {
        return accountId + "_" + orderId;
    }
}
