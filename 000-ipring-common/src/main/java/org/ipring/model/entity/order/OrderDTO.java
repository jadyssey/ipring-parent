package org.ipring.model.entity.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lgj
 * @date 2024/4/3
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private Long accountId;
    private Long orderId;

    public static OrderDTO of(Long accountId, Long orderId) {
        return new OrderDTO(accountId, orderId);
    }

    public String getUniqKey() {
        return this.accountId + "_" + this.orderId;
    }
}
