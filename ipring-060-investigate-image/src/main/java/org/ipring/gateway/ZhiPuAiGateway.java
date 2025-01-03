package org.ipring.gateway;

import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import org.ipring.model.ChatBody;
import org.ipring.model.common.Return;

public interface ZhiPuAiGateway {

    Return<String> completions(ChatBody chatBody);
}