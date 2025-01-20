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
import java.util.Map;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Component
@Slf4j
public class ChatGptGatewayImpl implements ChatGptGateway {
    @Resource
    private ChatGptApi chatGptApi;
    @Resource
    private  AzureChatGptApi azureChatGptApi;

    @Override
    public Return<ChatGPTResponse> completions(ChatCompletionRequest data) {
        String secretKey = HttpUtils.getHeader("secretKey");
        log.info("请求：{}", JsonUtils.toJson(data));
        ChatGPTResponse completions = chatGptApi.completions("Bearer " + secretKey, data);
        log.info("响应结果：{}", JsonUtils.toJson(completions));
        return ReturnFactory.success(completions);
    }

    @Override
    public Return<ChatGPTResponse> azureCompletions(ChatCompletionRequest data) {
        String apiKey = HttpUtils.getHeader("api-key");
        Map<String, Object> map = JsonUtils.toMap(data);
        log.info("请求：{}", JsonUtils.toJson(map));
        ChatGPTResponse completions;
        if (data.getModel().equals("gpt-4o")) {
            completions = azureChatGptApi.azureGpt4o(apiKey, map);
        } else {
            map.put("max_tokens", 800);
            completions = azureChatGptApi.azureGpt4oMini(apiKey, map);
        }
        log.info("响应结果：{}", JsonUtils.toJson(completions));
        return ReturnFactory.success(completions);
    }
}
