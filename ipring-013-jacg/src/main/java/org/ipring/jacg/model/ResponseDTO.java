package org.ipring.jacg.model;

import lombok.Data;
import java.util.Map;

@Data
public class ResponseDTO {
    private Integer status;
    private DataDTO data;
    private String errMsg;

    @Data
    public static class DataDTO {
        // 我们只需要订阅表
        private Map<String, SubscriptionDTO> subscriptionTable;
    }

    @Data
    public static class SubscriptionDTO {
        // 只需要 topic 名称
        private String topic;
    }
}