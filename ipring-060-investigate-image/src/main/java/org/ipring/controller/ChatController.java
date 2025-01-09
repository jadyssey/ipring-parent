package org.ipring.controller;

import cn.hutool.core.util.IdUtil;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.gateway.ZhiPuAiGateway;
import org.ipring.gateway.ZhiPuAiGatewayImpl;
import org.ipring.model.ChatBody;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.util.JsonUtils;
import org.ipring.util.ZhipuAI;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.ipring.gateway.ZhiPuAiGatewayImpl.SECRET_KEY;

/**
 * @Author lgj
 * @Date 2024/4/14
 */
@Api(tags = "校验测试接口")
@RequestMapping("/chat")
@RestController
@Validated
@RequiredArgsConstructor
public class ChatController {
    private final ZhiPuAiGateway zhiPuAiGateway;

    @PostMapping("/zhipu")
    @StlApiOperation(title = "chat zhipu", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<String> get(@RequestBody ChatBody chatBody) {
        // ModelApiResponse modelApiResponse = testInvoke(chatBody);
        ZhipuAI zhipuAI = new ZhipuAI(SECRET_KEY);
        String completion = zhipuAI.createCompletion("glm-4v-flash", chatBody.getImageUrl(), chatBody.getText());
        return ReturnFactory.success(completion);
    }

    @PostMapping("/zhipu/v2")
    @StlApiOperation(title = "chat zhipu", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<String> getV2(@RequestBody ChatBody chatBody) {
        return zhiPuAiGateway.completions(chatBody);
    }


    /**
     * 同步调用
     */
    private ModelApiResponse testInvoke(ChatBody chatBody) {
        ClientV4 client = new ClientV4.Builder(SECRET_KEY).build();

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), ZhiPuAiGatewayImpl.getImageChatContent(chatBody.getText(), chatBody.getImageUrl()));
        messages.add(chatMessage);
        String requestId = String.format(IdUtil.fastSimpleUUID(), System.currentTimeMillis());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model(Constants.ModelChatGLM4V)
                .stream(Boolean.FALSE).invokeMethod(Constants.invokeMethod).messages(messages).requestId(requestId).maxTokens(1024).topP(0.7F).build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        try {
            System.out.println("model output:" + JsonUtils.toJson(invokeModelApiResp));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return invokeModelApiResp;
    }
}
