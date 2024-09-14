package org.ipring.constant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SWAGGER配置
 *
 * @author lgj
 * @date 8/2/2023
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ipring.swagger")
public class SwaggerProperties {

    /**
     * 开启状态
     */
    private String enable;

    /**
     * 模块名
     */
    private String name;

    /**
     * 当前版本
     */
    private String version;
}
