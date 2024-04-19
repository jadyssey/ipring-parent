package org.ipring.constant.properties;

import org.ipring.constant.CommonConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 系统环境相关属性类
 *
 * @author lgj
 * @date 8/2/2023
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ipring.env")
public class EnvProperties {

    /**
     * 环境名称
     */
    private String name;

    /**
     * 当前环境访问的域名地址
     */
    private String host;

    private Config bv;

    private Config fb;

    @Data
    public static class Config {
        /**
         * 站点域名
         */
        private String web;

        /**
         * 官方邮箱
         */
        private String officialEmail;
    }

    public boolean isProdEnv() {
        return CommonConstants.PROD.equals(this.name);
    }
    public boolean isTestEnv() {
        return CommonConstants.TEST.equals(this.name);
    }

    public boolean isProdStageEnv() {
        return CommonConstants.PROD.equals(this.name) || CommonConstants.STAGE.equals(this.name);
    }
}
