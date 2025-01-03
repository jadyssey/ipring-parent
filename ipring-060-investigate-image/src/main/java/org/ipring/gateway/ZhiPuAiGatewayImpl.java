package org.ipring.gateway;

import cn.hutool.core.util.IdUtil;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.model.ChatBody;
import org.ipring.model.ImageExplanationRequest;
import org.ipring.model.ImageReq;
import org.ipring.model.common.Return;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liuguangjin
 * @date 2024/12/26
 */
@Component
@Slf4j
public class ZhiPuAiGatewayImpl implements ZhiPuAiGateway {
    public static final String SECRET_KEY = "ddd1ae28c5a6d76102a30d0c9bbf2cc4.LOoSzgyK3IS1hJVO";

    @Resource
    private ZhiPuAiApi zhiPuAiApi;

    @Override
    public Return<String> completions(ChatBody chatBody) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), getImageChatContent(chatBody.getText(), chatBody.getImageUrl()));
        messages.add(chatMessage);
        String requestId = String.format(IdUtil.fastSimpleUUID(), System.currentTimeMillis());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder().model("glm-4v-flash")
                .stream(Boolean.FALSE).invokeMethod(Constants.invokeMethod).messages(messages).requestId(requestId).maxTokens(1024).topP(0.7F).build();
        return zhiPuAiApi.completions("Bearer " + SECRET_KEY, chatCompletionRequest);
    }

    public static String getImageChatContent(String text, String imageUrl) {
        List<ImageExplanationRequest> requests = new ArrayList<>();

        ImageExplanationRequest requestImg = new ImageExplanationRequest();
        requestImg.setType("image_url");

        ImageReq imageReq = new ImageReq();

        imageReq.setUrl(imageUrl);
        requestImg.setImage_url(imageReq);
        requests.add(requestImg);

        ImageExplanationRequest requestText = new ImageExplanationRequest();
        requestText.setType("text");
        requestText.setText(text);
        requests.add(requestText);
        return JsonUtils.toJson(requests);
    }
}
