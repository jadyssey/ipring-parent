package org.ipring.gateway;

import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.model.gpt.ChatGPTResponse;
import org.ipring.util.HttpUtils;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Component
@Slf4j
public class ChatGptGatewayImpl implements ChatGptGateway {
    @Resource
    private ChatGptApi chatGptApi;

    @Override
    public Return<ChatGPTResponse> completions(ChatCompletionRequest data) {
        String secretKey = HttpUtils.getHeader("secretKey");
        log.info("请求：{}", JsonUtils.toJson(data));
        ChatGPTResponse completions = chatGptApi.completions("Bearer " + secretKey, data);
        log.info("响应结果：{}", completions);
        return ReturnFactory.success(completions);
    }
}
