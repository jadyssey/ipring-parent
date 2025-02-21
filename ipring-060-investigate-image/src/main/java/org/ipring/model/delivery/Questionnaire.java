package org.ipring.model.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Questionnaire {
    @JsonProperty("q1")
    private Boolean q1;

    @JsonProperty("q2")
    private String q2;

    @JsonProperty("q3")
    private String q3;

    @JsonProperty("q4")
    private Boolean q4;

    @JsonProperty("q5")
    private Boolean q5;

    @JsonProperty("q6")
    private Boolean q6;

    @JsonProperty("q7")
    private String q7;
}