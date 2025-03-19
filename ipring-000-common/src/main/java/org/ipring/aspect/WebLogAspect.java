package org.ipring.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.ipring.anno.StlApiOperation;
import org.ipring.util.HttpUtils;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.ipring.constant.LogConstant.*;

/**
 * @author Rainful
 * @date 2022/12/16 9:32
 */
@Component
@Aspect
@Slf4j
@Order(2)
public class WebLogAspect {

    @Pointcut(value = "execution(public * org.ipring.controller..*Controller.*(..))")
    public void webLog() {
    }

    @Pointcut("execution(public * org.ipring.exception.GlobalExceptionHandler.*(..))")
    public void exceptions() {
    }

    /**
     * 只在进入controller时记录请求信息
     */
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature method = (MethodSignature) signature;
            StlApiOperation operation = method.getMethod().getAnnotation(StlApiOperation.class);
            Optional<HttpServletRequest> requestOpt = HttpUtils.getRequest();
            if (!requestOpt.isPresent()) {
                log.debug("方法描述 {}", signature.getDeclaringTypeName() + ":" + signature.getName(), operation == null ? "此方法无描述" : operation.title());
                return;
            }
            HttpServletRequest request = requestOpt.get();
            log.debug("请求路径 {} ,进入方法 {}, 方法描述 {}",
                    request.getRequestURI() + ":" + request.getMethod(), signature.getDeclaringTypeName() + ":" + signature.getName(), operation == null ? "此方法无描述" : operation.title());
            MDC.put(REQ, getRequestInfo(request).toJSONString());
            MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
        } catch (Exception ex) {
            log.error("webLog 日志异常 => ", ex);
            throw ex;
        }
    }

    /**
     * 打印请求日志
     */
    @AfterReturning(pointcut = "webLog() || exceptions()", returning = "result")
    public void afterReturning(Object result) {
        try {
            Map<String, String> map = MDC.getCopyOfContextMap();
            if (map != null) {
                JSONObject jsonObject = new JSONObject(true);
                jsonObject.put(URI, HttpUtils.getRequest().map(HttpServletRequest::getRequestURI).orElse(""));
                jsonObject.put(TOOK, System.currentTimeMillis() - Long.parseLong(map.getOrDefault(START_TIME, String.valueOf(System.currentTimeMillis()))));
                jsonObject.put(UID, map.getOrDefault(UID, ""));
                jsonObject.put(REQ, JSON.parse(map.getOrDefault(REQ, "")));
                if (result != null) {
                    jsonObject.put("res", result);
                }
                log.info(jsonObject.toJSONString());
            }
        } catch (Exception ex) {
            log.error("webLog || exceptions 日志异常 => ", ex);
            throw ex;
        }
    }

    /**
     * 读取请求信息,转换为json
     */
    private JSONObject getRequestInfo(HttpServletRequest req) {
        JSONObject requestInfo = new JSONObject();
        try {
            StringBuffer requestURL = req.getRequestURL();
            requestInfo.put(REQ_URL, requestURL);
            String method = req.getMethod();
            requestInfo.put(METHOD, method);
            if (req.getQueryString() != null) {
                requestInfo.put(QUERY_STRING, URLDecoder.decode(req.getQueryString(), "UTF-8"));
            }
            requestInfo.put(REMOTE_ADDR, HttpUtils.getReqIp());
            if (req instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;
                String bodyStr = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                if (bodyStr.startsWith("{")) {
                    JSONObject jsonObject = JSON.parseObject(bodyStr);
                    requestInfo.put(REQ_BODY, jsonObject);
                }
            }
        } catch (Exception e) {
            log.error("解析请求失败", e);
            requestInfo.put("parseError", e.getMessage());
        }
        return requestInfo;
    }
}
