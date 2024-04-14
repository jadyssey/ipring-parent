package org.ipring.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Rainful
 * @date: 2024/04/08 15:25
 * @description:
 */
@Data
@ConfigurationProperties("topic")
public class TopicProperties {

    private String order;

    private String account;
}
