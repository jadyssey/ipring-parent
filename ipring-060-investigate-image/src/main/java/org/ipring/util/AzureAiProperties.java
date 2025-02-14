package org.ipring.util;

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
@ConfigurationProperties(prefix = "azure.openai")
public class AzureAiProperties {

    private String apiKey;
    private String endpoint;
}
