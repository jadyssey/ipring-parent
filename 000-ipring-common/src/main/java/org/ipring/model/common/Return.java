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
public class Return<T> {
    private Integer code;
    private String subCode;
    private String message;
    private T bodyMessage;

    public boolean success() {
        return ReturnFactory.check(this);
    }

    public boolean hashData() {
        if (this.bodyMessage == null) return false;
        return StringUtils.hasText(String.valueOf(this.bodyMessage));
    }
}