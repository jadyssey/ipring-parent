package org.ipring.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qinrongjun
 * @date: 2020/8/4 10:33 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties
public class BaseServiceResponse {

    private static final int SUCCESS_CODE = 0;

    private static final String SUCCESS_SUB_CODE_END = "00";

    private static final String SUCCESS_SUB_CODE = "0";

    private static final Integer SUB_CODE_MAX_LEN = 7;

    private int code;

    private String subCode;

    private String message;

    private String bodyMessage;

    private String data;


    public static boolean check(BaseServiceResponse response) {
        if (null == response) {
            return false;
        }
        return SUCCESS_CODE == response.getCode() && (response.getSubCode().endsWith(SUCCESS_SUB_CODE_END) || SUCCESS_SUB_CODE.equals(response.getSubCode()));
    }

    public static boolean check(BaseServiceResponse response, String... successSubCodeList) {
        if (null == response) {
            return false;
        }
        String subCode = dropPrefix(response.getSubCode());
        for (String item : successSubCodeList) {
            if (item.equals(subCode)) {
                return true;
            }
        }
        return false;
    }

    public static String dropPrefix(String subCode) {
        if (null == subCode) {
            return null;
        }
        Integer len = subCode.length();
        if (len > SUB_CODE_MAX_LEN) {
            return subCode.substring(len - SUB_CODE_MAX_LEN);
        }
        return subCode;
    }

}
