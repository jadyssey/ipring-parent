package org.ipring.model.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Questionnaire {
    @JsonProperty("q1")
    private String q1;

    @JsonProperty("q2")
    private String q2;

    @JsonProperty("q3")
    private String q3;

    @JsonProperty("q4")
    private String q4;

    @JsonProperty("q5")
    private String q5;

    @JsonProperty("q6")
    private String q6;

    @JsonProperty("q7")
    private String q7;
}