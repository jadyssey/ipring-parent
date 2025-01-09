package org.ipring.model.gemini;

import com.google.cloud.vertexai.api.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import com.google.type.Date;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResponseMapper {

    public static List<String> extractTextFromGeminiResponse(GeminiResponse geminiResponse) {
        List<String> textList = new ArrayList<>();
        // 遍历 candidates 列表
        for (GeminiResponse.Candidate candidate : geminiResponse.getCandidates()) {
            // 获取每个 Candidate 的 Content
            GeminiResponse.Content content = candidate.getContent();
            if (Objects.nonNull(content)) {
                // 遍历 Content 中的 parts 列表
                List<GeminiResponse.Part> parts = content.getParts();
                for (GeminiResponse.Part part : parts) {
                    // 获取 Part 的 text 字段并打印
                    textList.add(part.getText());
                }
            }
        }
        return textList;
    }

    public static GeminiResponse mapGenerateContentResponseToGeminiResponse(GenerateContentResponse generateContentResponse) {
        GeminiResponse geminiResponse = new GeminiResponse();

        // Map UsageMetadata
        geminiResponse.setUsageMetadata(mapUsageMetadata(generateContentResponse.getUsageMetadata()));

        // Map Candidates
        List<GeminiResponse.Candidate> candidates = generateContentResponse.getCandidatesList().stream().map(ResponseMapper::mapCandidate).collect(Collectors.toList());
        geminiResponse.setCandidates(candidates);
        geminiResponse.setUnknownFields(printUnknownFields(generateContentResponse.getUnknownFields()));
        return geminiResponse;
    }

    public static UnknownFieldSetCus printUnknownFields(UnknownFieldSet unknownFields) {
        UnknownFieldSetCus unknownFieldSetCus = new UnknownFieldSetCus();
        if (unknownFields != null) {
            for (Map.Entry<Integer, UnknownFieldSet.Field> entry : unknownFields.asMap().entrySet()) {
                UnknownFieldSet.Field value = entry.getValue();
                if (Objects.nonNull(value) && !value.getLengthDelimitedList().isEmpty()) {
                    // 处理 lengthDelimited 字段，通常是字节数组（ByteString）
                    StringBuilder sb = new StringBuilder();
                    for (ByteString byteString : value.getLengthDelimitedList()) {
                        sb.append(byteString.toStringUtf8()).append(",");
                    }
                    // 判断 StringBuilder 的长度是否大于 0，并且最后一个字符是否为逗号
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
                        // 删除最后一个字符
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    unknownFieldSetCus.addField(String.valueOf(entry.getKey()), sb);
                }
            }
        }
        return unknownFieldSetCus;
    }

    private static GeminiResponse.UsageMetadata mapUsageMetadata(GenerateContentResponse.UsageMetadata generateUsageMetadata) {
        GeminiResponse.UsageMetadata geminiUsageMetadata = new GeminiResponse.UsageMetadata();
        geminiUsageMetadata.setPromptTokenCount(generateUsageMetadata.getPromptTokenCount());
        geminiUsageMetadata.setCandidatesTokenCount(generateUsageMetadata.getCandidatesTokenCount());
        geminiUsageMetadata.setTotalTokenCount(generateUsageMetadata.getTotalTokenCount());
        return geminiUsageMetadata;
    }

    private static GeminiResponse.Candidate mapCandidate(Candidate generateCandidate) {
        GeminiResponse.Candidate geminiCandidate = new GeminiResponse.Candidate();

        // Map Content
        GeminiResponse.Content geminiContent = new GeminiResponse.Content();
        geminiContent.setParts(generateCandidate.getContent().getPartsList().stream().map(ResponseMapper::mapPart).collect(Collectors.toList()));
        geminiCandidate.setContent(geminiContent);

        // Map FinishReason
        geminiCandidate.setFinishReason(mapFinishReason(generateCandidate.getFinishReason()));

        // Map Safety Ratings
        geminiCandidate.setSafetyRatings(generateCandidate.getSafetyRatingsList().stream().map(ResponseMapper::mapSafetyRating).collect(Collectors.toList()));

        // Map CitationMetadata
        geminiCandidate.setCitationMetadata(mapCitationMetadata(generateCandidate.getCitationMetadata()));

        return geminiCandidate;
    }

    private static GeminiResponse.Part mapPart(Part generatePart) {
        GeminiResponse.Part geminiPart = new GeminiResponse.Part();
        geminiPart.setText(generatePart.getText());
        return geminiPart;
    }

    private static String mapFinishReason(Candidate.FinishReason generateFinishReason) {
        return generateFinishReason.name();
        /*switch (generateFinishReason) {
            case STOP:
                return GeminiResponse.FinishReason.STOP;
            // todo
            default:
                return null;
            // throw new IllegalArgumentException("Unknown finish reason: " + generateFinishReason);
        }*/
    }

    private static GeminiResponse.SafetyRating mapSafetyRating(SafetyRating generateSafetyRating) {
        GeminiResponse.SafetyRating geminiSafetyRating = new GeminiResponse.SafetyRating();
        geminiSafetyRating.setCategory(mapHarmCategory(generateSafetyRating.getCategory()));
        geminiSafetyRating.setProbability(mapHarmProbability(generateSafetyRating.getProbability()));
        geminiSafetyRating.setBlocked(generateSafetyRating.getBlocked());
        return geminiSafetyRating;
    }

    private static String mapHarmCategory(HarmCategory generateHarmCategory) {
        return generateHarmCategory.name();
        /*switch (generateHarmCategory) {
            case HARM_CATEGORY_UNSPECIFIED:
                return GeminiResponse.HarmCategory.VIOLENCE;
            default:
                return null;
                // throw new IllegalArgumentException("Unknown harm category: " + generateHarmCategory);
        }*/
    }

    private static String mapHarmProbability(SafetyRating.HarmProbability generateHarmProbability) {
        return generateHarmProbability.name();
        /*switch (generateHarmProbability) {
            case HIGH:
                return GeminiResponse.HarmProbability.HIGH;
            case MEDIUM:
                return GeminiResponse.HarmProbability.MEDIUM;
            case LOW:
                return GeminiResponse.HarmProbability.LOW;
            default:
                return null;
                // throw new IllegalArgumentException("Unknown harm probability: " + generateHarmProbability);
        }*/
    }

    private static GeminiResponse.CitationMetadata mapCitationMetadata(CitationMetadata generateCitationMetadata) {
        GeminiResponse.CitationMetadata geminiCitationMetadata = new GeminiResponse.CitationMetadata();
        geminiCitationMetadata.setCitations(generateCitationMetadata.getCitationsList().stream().map(ResponseMapper::mapCitation).collect(Collectors.toList()));
        return geminiCitationMetadata;
    }

    private static GeminiResponse.Citation mapCitation(Citation generateCitation) {
        GeminiResponse.Citation geminiCitation = new GeminiResponse.Citation();
        geminiCitation.setStartIndex(generateCitation.getStartIndex());
        geminiCitation.setEndIndex(generateCitation.getEndIndex());
        geminiCitation.setUri(generateCitation.getUri());
        geminiCitation.setTitle(generateCitation.getTitle());
        geminiCitation.setLicense(generateCitation.getLicense());
        geminiCitation.setPublicationDate(mapPublicationDate(generateCitation.getPublicationDate()));
        return geminiCitation;
    }

    private static GeminiResponse.PublicationDate mapPublicationDate(Date generatePublicationDate) {
        GeminiResponse.PublicationDate geminiPublicationDate = new GeminiResponse.PublicationDate();
        geminiPublicationDate.setYear(generatePublicationDate.getYear());
        geminiPublicationDate.setMonth(generatePublicationDate.getMonth());
        geminiPublicationDate.setDay(generatePublicationDate.getDay());
        return geminiPublicationDate;
    }
}
