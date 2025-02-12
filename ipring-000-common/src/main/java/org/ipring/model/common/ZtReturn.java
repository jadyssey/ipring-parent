package org.ipring.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * @Author lgj
 * @Date 2024/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZtReturn<T> {
    private Integer code;
    private String message;
    private T data;

    public boolean success() {
        return ReturnFactory.check(this);
    }

    public boolean hashData() {
        if (this.data == null) return false;
        return StringUtils.hasText(String.valueOf(this.data));
    }
}