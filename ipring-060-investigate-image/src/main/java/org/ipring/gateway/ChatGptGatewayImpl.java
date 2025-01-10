package org.ipring.gateway;

import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.ipring.model.common.Return;
import org.ipring.util.HttpUtils;
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
    public Return<String> completions(ChatCompletionRequest data) {
        String secretKey = HttpUtils.getHeader("secretKey");
        return chatGptApi.completions("Bearer " + secretKey, data);
    }
}
