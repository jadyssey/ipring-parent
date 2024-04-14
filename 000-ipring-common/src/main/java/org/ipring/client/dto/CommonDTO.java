package org.ipring.client.dto;

import lombok.Data;

/**
 * @author: qinrongjun
 * @date: 2020/8/20 3:39 下午
 */
@Data
public class CommonDTO<T> {

    /**
     * 成功
     */
    public static final String SUCCESS = "0";

    /**
     * 失败
     */
    public static final String ERROR = "-1";


    /**
     * 状态码
     */
    public String status;

    /**
     * 返回结果
     */
    private T data;

    /**
     * 错误信息
     */
    private String errMsg;


    public boolean isSuccess() {
        return SUCCESS.equals(status);
    }

    public static CommonDTO<?> success() {
        return success(SUCCESS, null);
    }

    public static <T> CommonDTO<T> success(T result) {
        return success(SUCCESS, result);
    }

    public static <T> CommonDTO<T> success(String status, T result) {
        CommonDTO<T> resultDTO = new CommonDTO<>();
        resultDTO.setStatus(status);
        resultDTO.setData(result);
        return resultDTO;
    }

    public static <T> CommonDTO<T> error(String errMsg) {
        CommonDTO<T> resultDTO = new CommonDTO<>();
        resultDTO.setStatus(ERROR);
        resultDTO.setErrMsg(errMsg);
        return resultDTO;
    }

    public static <T> CommonDTO<T> error(String status, String errMsg) {
        CommonDTO<T> resultDTO = new CommonDTO<>();
        resultDTO.setStatus(status);
        resultDTO.setErrMsg(errMsg);
        return resultDTO;
    }

 }
