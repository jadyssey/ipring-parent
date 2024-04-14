package org.ipring.util;

import org.ipring.constant.AuthConstant;
import org.ipring.enums.common.ClientTypeInt;
import org.ipring.enums.common.LangTypeStr;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * 和http相关的工具类
 *
 * @author lgs
 */
@Slf4j
public class HttpUtils {

    /**
     * 得到 request 对象
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    /**
     * 获取header数据
     *
     * @param header 请求头名称
     */
    public static String getHeader(String header) {
        return getRequest().getHeader(header);
    }

    /**
     * 获取当前用户id
     *
     * @return
     */
    public static Long getUserId() {
        try {
            String uidStr = getHeader(AuthConstant.HEADER_UID);
            return StringUtils.isNotBlank(uidStr) ? Long.parseLong(uidStr) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static String getStrUserId() {
        return getUserId().toString();
    }

    /**
     * 获取请求的客户端类型
     *
     * @return 请求头携带的客户端类型
     * @see ClientTypeEnum
     */
    // public static int getClientType() {
    //     try {
    //         String clientType = getHeader(RequestHeaderParam.HEADER_CLIENT_TYPE);
    //         if (StringUtils.isEmpty(clientType)) {
    //             return ClientTypeEnum.UNKNOWN.getCode();
    //         }
    //         return Integer.parseInt(clientType);
    //     } catch (NumberFormatException e) {
    //         return ClientTypeEnum.UNKNOWN.getCode();
    //     }
    // }

    /**
     * 获取请求IP
     */
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-REAL-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public static String getReqIp() {
        HttpServletRequest request = getRequest();
        String ipTest = request.getHeader("ip-test");
        if (org.springframework.util.StringUtils.hasText(ipTest)) {
            return ipTest;
        }
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
                String ip = ipList.split(",")[0];
                if (org.springframework.util.StringUtils.hasText(ip)) return ip;
            }
        }

        String ipAddress = request.getRemoteAddr();
        if ("127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                return inetAddress.getHostAddress();
            } catch (UnknownHostException e) {
                return ipAddress;
            }
        }
        return ipAddress;
    }

    /**
     * 获取此次请求的多语言对应的code
     */
    // public static LanguageCode getLanguageCode() {
    //     Locale locale = LocaleContextHolder.getLocale();
    //     LanguageCode languageCode = LanguageCode.lowerAssoCodeOf(locale.toString().toLowerCase());
    //     return null == languageCode ? ENGLISH : languageCode;
    // }
    public static String getLangStr() {
        return Optional.ofNullable(getHeader(AuthConstant.LANGUAGE_HEADER)).map(String::toLowerCase).orElse(LangTypeStr.EN_US.getType());
    }

    /**
     * @return 请求头携带的客户端类型
     * @see ClientTypeInt
     */
    public static Integer getClientType() {
        return Optional.ofNullable(getHeader(AuthConstant.CLIENT_TYPE_HEADER)).map(Integer::parseInt).orElse(ClientTypeInt.OTHER.getType());
    }

    public static String getDeviceId() {
        return getHeader(AuthConstant.DEVICE_ID_HEADER);
    }
}
