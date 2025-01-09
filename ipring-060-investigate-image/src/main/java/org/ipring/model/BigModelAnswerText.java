package org.ipring.model;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;
import org.ipring.model.gemini.GeminiResponse;
import org.ipring.util.JsonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2025/1/8
 */
@Data
public class BigModelAnswerText {
    private List<String> sourceTextList;

    private Map<String, Boolean> map;
    private List<String> list;

    private GeminiResponse.UsageMetadata usageMetadata;

    public Map<String, Boolean> getMap() {
        try {
            if (CollectionUtil.isNotEmpty(this.getSourceTextList()) && this.getSourceTextList().size() == 1) {
                return (Map<String, Boolean>) Optional.ofNullable(this.getSourceTextList().get(0)).map(tx -> JsonUtils.toObject(tx, Map.class)).orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<String> getList() {
        try {
            if (CollectionUtil.isNotEmpty(this.getSourceTextList()) && this.getSourceTextList().size() == 1) {
                return Optional.ofNullable(this.getSourceTextList().get(0)).map(tx -> tx.split("::")).map(tx -> Arrays.stream(tx).collect(Collectors.toList())).orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
