package org.ipring.model.gpt;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatGPTResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;
        private Object logprobs;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        private Object refusal;
    }

    @Data
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
        private Map<String, Integer> prompt_tokens_details;
        private Map<String, Integer> completion_tokens_details;
    }
}
