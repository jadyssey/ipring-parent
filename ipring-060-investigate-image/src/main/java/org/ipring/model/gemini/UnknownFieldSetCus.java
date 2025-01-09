package org.ipring.model.gemini;

import java.util.HashMap;
import java.util.Map;

public class UnknownFieldSetCus {
    private final Map<String, Object> fields = new HashMap<>();

    // 获取字段
    public Object getField(String fieldName) {
        return fields.get(fieldName);
    }

    // 获取所有字段
    public Map<String, Object> getFields() {
        return fields;
    }

    // 添加字段
    public void addField(String fieldName, Object fieldValue) {
        fields.put(fieldName, fieldValue);
    }
}