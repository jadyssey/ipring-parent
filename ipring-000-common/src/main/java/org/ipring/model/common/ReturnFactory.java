package org.ipring.model.common;

import org.ipring.enums.SubCode;
import org.ipring.enums.subcode.SystemServiceCode;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author lgj
 * @date 2024/4/1
 **/
public class ReturnFactory {
    private static final int SUCCESS = 0;
    private static final int ERROR = -1;
    private static final String SUCCESS_SUB_CODE_END = "00";

    public static <T> Return<T> success() {
        Return<T> r = new Return<>();
        r.setCode(SUCCESS);
        r.setSubCode(SystemServiceCode.SystemApi.SUCCESS.getSubCode());
        r.setMessage(SystemServiceCode.SystemApi.SUCCESS.getI18nMsg());
        return r;
    }

    public static <T> Return<T> success(T data) {
        Return<T> r = new Return<>();
        r.setCode(SUCCESS);
        r.setSubCode(SystemServiceCode.SystemApi.SUCCESS.getSubCode());
        r.setMessage(SystemServiceCode.SystemApi.SUCCESS.getI18nMsg());
        r.setBodyMessage(data);
        return r;
    }

    /**
     * 返回错误模型，自定义code和message
     *
     * @param subCode
     * @param <T>
     * @return
     */
    public static <T> Return<T> info(SubCode subCode) {
        Return<T> r = new Return<>();
        r.setCode(SUCCESS);
        r.setSubCode(subCode.getSubCode());
        r.setMessage(subCode.getI18nMsg());
        return r;
    }

    /**
     * 操作频繁
     * @return
     */
    public static Return<?> frequently() {
        return info(SystemServiceCode.SystemApi.WAIT_MIN);
    }

    /**
     * 返回错误subcode
     * @param message 自定义message
     * @param subCode 自定义subcode
     * @param <T>
     * @return
     */
    public static <T> Return<T> info(String message, String subCode) {
        Return<T> r = new Return<>();
        r.setCode(SUCCESS);
        r.setSubCode(subCode);
        r.setMessage(message);
        return r;
    }

    /**
     * 返回错误模型，
     *
     * @param <T>
     * @return
     */
    public static <T> Return<T> error() {
        Return<T> r = new Return<>();
        r.setCode(ERROR);
        r.setSubCode(SystemServiceCode.SystemApi.FAIL.getSubCode());
        r.setMessage(SystemServiceCode.SystemApi.FAIL.getI18nMsg());
        return r;
    }

    public static boolean check(Return<?> resp) {
        if (null == resp) return false;
        return resp.getCode().equals(SUCCESS) && StringUtils.hasText(resp.getSubCode()) && resp.getSubCode().endsWith(SUCCESS_SUB_CODE_END);
    }
    public static boolean check(ZtReturn<?> resp) {
        if (null == resp || Objects.isNull(resp.getCode())) return false;
        return resp.getCode().equals(200);
    }
}
