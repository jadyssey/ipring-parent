package org.ipring.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * clientType: 统一用common_service的"ClientTypeInt"枚举
 * clientVersion：移动端版本号（如果没有传latest）
 * deviceId: 32位的设备Id （如果没有传none）
 * bToken：统一用common_service配置文件中的uuid生成bToken
 *
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

    public static final String HEADER_UID = "uid";

    public static final String HEADER_USER_NAME = "username";

    /**
     * 请求的客户端类型
     */
    public static final String CLIENT_TYPE_HEADER = "clientType";

    /**
     * 头字段：clientVersion 客户端版本号，Web/h5/server（如果没有传latest）
     */
    public static final String CLIENT_VERSION_HEADER = "clientVersion";

    /**
     * 头字段：deviceId （如果没有传none）
     */
    public static final String DEVICE_ID_HEADER = "deviceId";

    /**
     * bToken组成规则:  clientType_clientVersion_uuid_deviceId  并对该参数进行MD5加密
     * <p>
     * 其中uuid为固定的32位的字符
     */
    public static final String BASE_TOKEN_HEADER = "bToken";

    /**
     * 语言类型 header 名称
     */
    public static final String LANGUAGE_HEADER = "locale";

    /**
     * 产品类型
     */
    public static final String PRODUCT_TYPE_HEADER = "productType";

    /**
     * 头字段：当前时间戳，毫秒，timestamp的简写
     */
    public static final String TS = "ts";
    /**
     * 头字段：一个随机数字，6位，random的简写
     */
    public static final String RD = "rd";

    /**
     * 头字段：按照 MD5((productType_ts_rd)).toLowerCase()，signature的简写
     */
    public static final String SG = "sg";

    /**
     * 来自swagger访问，忽略权限校验
     */
    public static final String FROM_SWAGGER = "fromSwagger";

    public static final String ACC_ID = "accId";

}
