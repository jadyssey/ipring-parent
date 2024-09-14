package org.ipring.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.model.common.LogEntity;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.util.HttpUtils;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author lgj
 * @date 8/2/2023
 */
@Slf4j
@RestControllerAdvice
@Component
public class GlobalExceptionHandler {
    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Return<?> handleRuntimeException(RuntimeException e) {
        Return<?> returnModel = ReturnFactory.error();
        printErrorLog("RuntimeException", e, returnModel);
        return returnModel;
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Return<?> handleException(Exception e) {
        Return<?> returnModel = ReturnFactory.error();
        printErrorLog("Exception", e, returnModel);
        return returnModel;
    }

    /**
     * 权限码异常
     */
    @ExceptionHandler(ServiceException.class)
    public Return<?> handleNotPermissionException(ServiceException e) {
        Return<?> returnModel = ReturnFactory.info(e.getSubCode());
        printErrorLog("ServiceException", e, returnModel);
        return returnModel;
    }


    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Return<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        Return<?> returnModel = ReturnFactory.info(SystemServiceCode.SystemApi.PARAM_ERROR);
        printErrorLog("HttpRequestMethodNotSupportedException", e, returnModel);
        return returnModel;
    }

    /**
     * 统一处理请求参数校验 (实体对象传参)
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public Return<?> handleBindException(Exception e) {
        // 拼接错误信息
        String message = "";
        try {
            if (e instanceof MethodArgumentNotValidException) {
                message = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream().map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.joining(","));
            } else if (e instanceof BindException) {
                message = ((BindException) e).getBindingResult().getFieldErrors().stream().map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.joining(","));
            }
        } catch (Exception exception) {
            message = exception.getMessage();
            log.info("error = {}", message);
        }

        Return<?> returnModel = ReturnFactory.info(message, SystemServiceCode.SystemApi.PARAM_ERROR.getSubCode());
        printErrorLog("BindException", e, returnModel);
        return returnModel;
    }

    /*@ExceptionHandler({ConstraintViolationException.class})
    public Return handleConstraintViolationException(ConstraintViolationException e) {
        Return returnModel = ReturnModel.info(SystemServiceCode.SystemApi.PARAM_ERROR);
        printErrorLog("handleConstraintViolationException", e, returnModel);
        return returnModel;
    }*/

    private void printErrorLog(String exceptionName, Throwable e, Return returnModel) {
        HttpServletRequest request = HttpUtils.getRequest();

        LogEntity logEntity = LogEntity.init();
        logEntity.setParams(convertMap(request.getParameterMap()));
        logEntity.setErrorMessage(stackTraceToString(e.getClass().getName(), e.getMessage(), e.getStackTrace()));
        //logEntity.setResponse(JsonUtils.toJson(returnModel));
        log.error("[Catch {}, URI={} {}] {}", exceptionName, request.getRequestURI(), request.getMethod(), logEntity);
    }

    /**
     * 转换request 请求参数
     *
     * @param paramMap request获取的参数数组
     */
    public String convertMap(Map<String, String[]> paramMap) {
        Map<String, String> rtnMap = new HashMap<>(paramMap.size());
        for (String key : paramMap.keySet()) {
            rtnMap.put(key, paramMap.get(key)[0]);
        }
        return JsonUtils.toJson(rtnMap);
    }

    /**
     * 转换异常信息为字符串
     *
     * @param exceptionName    异常名称
     * @param exceptionMessage 异常信息
     * @param elements         堆栈信息
     */
    public String stackTraceToString(String exceptionName, String exceptionMessage, StackTraceElement[] elements) {
        StringBuilder buffer = new StringBuilder();
        for (StackTraceElement stet : elements) {
            buffer.append("\n").append(stet);
        }
        String subString = StringUtils.substring(buffer.toString(), 0, 2000);
        return "[" + exceptionName + "]" + exceptionMessage + ":" + subString;
    }
}
