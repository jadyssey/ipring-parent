package org.ipring.websocket.config;

import org.ipring.websocket.WebsocketServer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author: Rainful
 * @date: 2024/04/03 13:48
 * @description:
 */
@Configuration
@EnableConfigurationProperties(WebsocketProperties.class)
@Import(WebsocketServer.class)
public class WebsocketConfig {


}
