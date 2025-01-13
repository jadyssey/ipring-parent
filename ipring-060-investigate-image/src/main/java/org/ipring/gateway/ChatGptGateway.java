package org.ipring.gateway;

import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import org.ipring.model.common.Return;
import org.ipring.model.gpt.ChatGPTResponse;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
public interface ChatGptGateway {
    Return<ChatGPTResponse> completions(ChatCompletionRequest data);
}
