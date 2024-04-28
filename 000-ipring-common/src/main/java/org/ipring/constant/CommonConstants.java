package org.ipring.constant;

import io.netty.util.AttributeKey;

import java.util.Set;

/**
 * @author lgj
 * @date 9/2/2023
 **/
public class CommonConstants {

    public static final String PROD = "prod";
    public static final String TEST = "test";
    public static final String DEV = "dev";
    public static final String STAGE = "stage";

    public static final String NONE = "none";

    public static final String OUTER_CLASS_SPLIT = "$";

    public static final String HTTPS = "https://";
    /**
     * 邮箱格式的正则表达式
     */
    public static final String REGEX_EMAIL = "^[-.\\w]+@[-\\w]+(\\.[-\\w]{2,}){1,3}$";
    /**
     * 邮箱字符限制数
     */
    public static final int EMAIL_LENGTH = 50;

    /**
     * 品种报价缓存间隔
     * 单位：毫秒
     */
    public static final int SYMBOL_MSG_CACHE_INTERVAL = 5 * 1000;

    /**
     * 最大待持久化的品种报价队列元素大小
     */
    public static final int SYMBOL_MSG_CACHE_SIZE = 1000;

    /**
     * 品种代码分割后的数组长度
     */
    public static int SYMBOL_LENTH = 2;

    /**
     * 组成品种唯一键的链接符号
     */
    public static String SYMBOL_SPLIT = "_";

    public static String ORDER_UNIQ_SPLIT = "_";

    public final static AttributeKey<String> TOKEN_KEY = AttributeKey.valueOf(AuthConstant.TOKEN);
    public final static AttributeKey<Set<Long>> ACC_KEY = AttributeKey.valueOf(AuthConstant.ACC_ID);
}
