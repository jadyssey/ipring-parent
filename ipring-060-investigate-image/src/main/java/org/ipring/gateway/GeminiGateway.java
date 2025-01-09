package org.ipring.gateway;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import org.ipring.model.ChatBody;

/**
 * @author liuguangjin
 * @date 2025/1/8
 */
public interface GeminiGateway {
    GenerateContentResponse gemini15pro(ChatBody chatBody);
}
