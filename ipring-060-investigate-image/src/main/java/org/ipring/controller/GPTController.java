package org.ipring.controller;

import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.gateway.ChatGptGateway;
import org.ipring.gateway.ZhiPuAiGatewayImpl;
import org.ipring.model.ChatBody;
import org.ipring.model.common.Return;
import org.ipring.model.gpt.ChatGPTResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Api(tags = "chatgpt接口")
@RequestMapping("/chat")
@RestController
@Validated
@RequiredArgsConstructor
public class GPTController {
    private final ChatGptGateway chatGptGateway;
    @PostMapping("/4o-mini")
    @StlApiOperation(title = "40-mini", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<ChatGPTResponse> get(@RequestBody ChatBody chatBody) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel("gpt-4o-mini");

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), ZhiPuAiGatewayImpl.getImageChatContent(chatBody.getText(), chatBody.getImageUrl()));
        messages.add(chatMessage);
        chatCompletionRequest.setMessages(messages);
        return chatGptGateway.completions(chatCompletionRequest);
    }
}
