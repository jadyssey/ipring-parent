package org.ipring.jacg.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author liuguangjin
 * @date 2025/12/23
 **/

@Data
public class ApmUriVO {
    @JsonProperty("pId")
    private String pId;

    @JsonProperty("formatValue")
    private String formatValue;

    @JsonProperty("duration_total_caller_percent")
    private Double durationTotalCallerPercent;

    @JsonProperty("jserror_top5_uri_percent")
    private Double jserrorTop5UriPercent;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("actionId")
    private Long actionId;

    @JsonProperty("serviceFlag")
    private Boolean serviceFlag;

    @JsonProperty("value")
    private Double value;
}
