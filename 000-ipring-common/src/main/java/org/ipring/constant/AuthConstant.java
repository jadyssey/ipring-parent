package org.ipring.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * clientType: 统一用common_service的"ClientTypeInt"枚举
 * clientVersion：移动端版本号（如果没有传latest）
 * deviceId: 32位的设备Id （如果没有传none）
 * bToken：统一用common_service配置文件中的uuid生成bToken
 * <p>
 * language：统一用common_service的的"LangTypeStr"枚举
 * productType：什么项目调用就传什么
 * ts：当前时间戳，13位毫秒
 * rd：一个随机数字，6位
 * sg：生成规则是 MD5((productType_ts_rd)).toLowerCase()
 *
 * @author lgj
 * @date 8/2/2023
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthConstant {

    public static final String TRACE_ID = "traceId";
    public static final String REQ_IP = "reqIp";
    public static final String UID = "uid";
    public static final String TOKEN = "ct4Token";
    public static final String REQ = "req";
    public static final String START_TIME = "startTime";
    public static final String TOOK = "took";
    public static final String URI = "uri";
    public static final String REQ_URL = "requestURL";
    public static final String METHOD = "method";
    public static final String QUERY_STRING = "queryString";
    public static final String REMOTE_ADDR = "remoteAddr";
    public static final String REQ_BODY = "requestBody";

    /**
     * 请求的客户端类型
     */
    public static final String CLIENT_TYPE_HEADER = "clientType";

    /**
     * 语言类型 header 名称
     */
    public static final String LANGUAGE_HEADER = "locale";

    /**
     * 产品类型
     */
    public static final String PRODUCT_TYPE_HEADER = "productType";

    /**
     * 来自swagger访问，忽略权限校验
     */
    public static final String FROM_SWAGGER = "fromSwagger";

    public static final String ACC_ID = "accId";
}
