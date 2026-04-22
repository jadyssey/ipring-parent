package org.ipring.jacg.model;

import lombok.Data;
import java.util.List;

@Data
public class MetricResponse {
    private String status;
    private DataDTO data;

    @Data
    public static class DataDTO {
        private String resultType;
        private List<ResultDTO> result;
    }

    @Data
    public static class ResultDTO {
        private MetricDTO metric;

        private List<String> value;
    }

    @Data
    public static class MetricDTO {
        // 只需要 topic 名称
        private String topic;
    }
}