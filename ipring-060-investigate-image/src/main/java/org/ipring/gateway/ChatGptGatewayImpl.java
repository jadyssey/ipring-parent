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
        Map<String, Object> map = JsonUtils.toMap(data);
        map.put("max_tokens", 5000);
        log.info("请求体：{}", JsonUtils.toJson(data));
        ChatGPTResponse completions = completionsAndTry(secretKey, map, 2);
        log.info("响应结果：{}", JsonUtils.toJson(completions));
        return ReturnFactory.success(completions);
    }

    public ChatGPTResponse completionsAndTry(String secretKey, Map<String, Object> map, Integer count) {
        try {
            return chatGptApi.completions("Bearer " + secretKey, map);
        } catch (Exception e) {
            log.info("调用失败，当前count ={}", count);
            if (count > 1) {
                return completionsAndTry(secretKey, map, --count);
            } else {
                throw e;
            }
        }
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
            String result = azureChatGptApi.azureGpt4oMini(apiKey, map);
            log.info("azureChatGptApi.azureGpt4oMini 返回结果：{}", result);
            completions = JsonUtils.toObject(result, ChatGPTResponse.class);
        }
        log.info("响应结果：{}", JsonUtils.toJson(completions));
        return ReturnFactory.success(completions);
    }
}
