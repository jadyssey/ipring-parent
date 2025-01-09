package org.ipring.model;// GeminiResponse.java
import lombok.Data;

import java.util.List;

@Data
public class GeminiResponse {
    private List<Candidate> candidates;
    private UsageMetadata usageMetadata;
    private UnknownFieldSetCus unknownFields;  // 用于存储未知字段


    // Nested Classes
    @Data
    public static class Candidate {
        private Content content;
        private String finishReason;
        private List<SafetyRating> safetyRatings;
        private CitationMetadata citationMetadata;
    }

    @Data
    public static class Content {
        private List<Part> parts;
    }

    @Data
    public static class Part {
        private String text;
    }

    public enum FinishReason {
        STOP, LENGTH, ERROR // 根据实际情况定义
    }

    /**
     * category: 有害内容的类别。这里列出了四种常见类别：
     * 1. HARM_CATEGORY_HATE_SPEECH：仇恨言论，指针对特定群体基于其种族、宗教、国籍、性取向等特征表达仇恨或歧视的言论。
     * 2. HARM_CATEGORY_DANGEROUS_CONTENT：危险内容，指可能导致人身伤害、财产损失或其他危险的言论，例如教唆暴力、提供制造炸弹的说明等。
     * 3. HARM_CATEGORY_HARASSMENT：骚扰，指对个人进行的具有攻击性、侮辱性或威胁性的言论，例如网络欺凌、人身攻击等。
     * 4. HARM_CATEGORY_SEXUALLY_EXPLICIT：色情内容，指包含露骨的性描写或图像的内容。
     *
     * probability: 模型评估该类别风险的概率级别。可能的取值包括：
     * 1. NEGLIGIBLE（可忽略不计）：风险极低。
     * 2. LOW（低）：风险较低。
     * 3. MEDIUM（中）：风险中等。
     * 4. HIGH（高）：风险较高。
     *
     * blocked: 指示该类别是否触发了内容屏蔽。如果为 true，表示模型的输出由于违反安全策略而被屏蔽。在这个例子中，所有类别的 blocked 值都为 false，表示没有内容被屏蔽。
     */
    @Data
    public static class SafetyRating {
        private String category;
        private String probability;
        private boolean blocked;
    }

    @Data
    public static class CitationMetadata {
        private List<Citation> citations;
    }

    @Data
    public static class Citation {
        private int startIndex;
        private int endIndex;
        private String uri;
        private String title;
        private String license;
        private PublicationDate publicationDate;
    }

    @Data
    public static class PublicationDate {
        private int year;
        private int month;
        private int day;
    }

    /**
     * prompt_token_count: 用户提供的提示中包含的令牌数量。
     * candidates_token_count: 模型生成的所有候选响应中包含的令牌数量。
     * total_token_count: 提示和所有候选响应中的令牌总数。
     */
    @Data
    public static class UsageMetadata {
        private int promptTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;
    }
}
