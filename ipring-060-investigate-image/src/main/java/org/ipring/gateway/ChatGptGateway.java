package org.ipring.gateway;

import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import org.ipring.model.common.Return;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
public interface ChatGptGateway {
    Return<String> completions(ChatCompletionRequest data);
}
