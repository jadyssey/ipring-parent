package org.ipring.model.entity.order;

import org.ipring.constant.CommonConstants;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Objects;

/**
 * @author lgj
 * @date 2024/4/8
 **/
@Data
public class OrderTickDTO extends TradeOrderEntity implements Comparable<OrderTickDTO> {

    /**
     * todo 精简字段
     */
    public static OrderTickDTO of(TradeOrderEntity order) {
        OrderTickDTO entity = new OrderTickDTO();
        BeanUtils.copyProperties(order, entity);
        return entity;
    }


    public String getSymbolUniq() {
        return this.getMarketType() + CommonConstants.SYMBOL_SPLIT + this.getSymbolId();
    }

    /**
     * 获取当前订单唯一键
     *
     * @return
     */
    public String getOrderUniq() {
        return getOrderByUniq(this.getAccountId(), this.getOrderId());
    }


    public static String getOrderByUniq(Long accountId, Long orderId) {
        return accountId + "_" + orderId;
    }

    @Override
    public int compareTo(OrderTickDTO other) {
        int compare = Long.compare(this.getAccountId(), other.getAccountId());
        if (compare == 0) compare = Long.compare(this.getOrderId(), other.getOrderId());
        return compare;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getAccountId(), getOrderId());
    }

    @Override
    public boolean equals(Object obj) {
        // 如果是同一个对象，返回 true
        if (this == obj) {
            return true;
        }
        // 如果 obj 为空，或者与当前对象类型不同，返回 false
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        // 将 obj 强制转换为当前对象的类型
        OrderTickDTO other = (OrderTickDTO) obj;
        // 比较对象的属性
        return Objects.equals(this.getAccountId(), other.getAccountId()) &&
                Objects.equals(this.getOrderId(), other.getOrderId());
    }
}