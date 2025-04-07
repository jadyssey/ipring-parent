package org.ipring.gateway.azure;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import org.apache.commons.lang3.StringUtils;
import org.ipring.model.ChatBody;
import org.ipring.util.AzureAiProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OpenAIService {

    @Resource
    private AzureAiProperties azureAiProperties;

    private static OpenAIClient client;

    @PostConstruct
    public void init() {
        // 初始化 OpenAIClient
        client = initOpenAIClient();
    }

    private OpenAIClient initOpenAIClient() {
        try {
            return new OpenAIClientBuilder()
                    .endpoint(azureAiProperties.getEndpoint())
                    .credential(new AzureKeyCredential(azureAiProperties.getApiKey()))
                    .buildClient();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenAIClient", e);
        }
    }

    /**
     * 带图片的消息
     *
     * @param chatBody
     * @return
     */
    public ChatCompletions getImageResp(ChatBody chatBody) {
        List<String> imageList = Optional.ofNullable(chatBody.getImageList()).orElse(new ArrayList<>());
        Optional.ofNullable(chatBody.getImageUrl()).filter(StringUtils::isNotBlank).ifPresent(imageList::add);

        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        // if (StringUtils.isNotBlank(chatBody.getSystemSetup())) {
            // 添加系统消息
            // chatMessages.add(new ChatRequestSystemMessage(chatBody.getSystemSetup()));
        // }

        // 创建包含文本和图片的用户消息列表
        List<ChatMessageContentItem> contentItems = new ArrayList<>();

        // 添加图片内容
        imageList.forEach(imageUrl -> {
            ChatMessageImageUrl chatMessageImageUrl = new ChatMessageImageUrl(imageUrl);
            chatMessageImageUrl.setDetail(ChatMessageImageDetailLevel.LOW);
            ChatMessageImageContentItem imageContentItem = new ChatMessageImageContentItem(chatMessageImageUrl);
            contentItems.add(imageContentItem);
        });

        // 添加文本内容
        contentItems.add(new ChatMessageTextContentItem(chatBody.getText()));

        // 创建用户消息并设置内容
        chatMessages.add(new ChatRequestUserMessage(contentItems));

        // 设置响应格式
        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);

        ChatCompletionsResponseFormat jsonResponseFormat = getChatCompletionsResponseFormat(chatBody.getJsonResponseFormat());
        chatCompletionsOptions.setResponseFormat(jsonResponseFormat);
        // chatCompletionsOptions.setTemperature() todo
        // 调用 OpenAI 接口获取聊天完成结果
        return client.getChatCompletions(chatBody.getModel(), chatCompletionsOptions);
    }

    private static ChatCompletionsJsonSchemaResponseFormat getChatCompletionsResponseFormat(String jsonResponseFormat) {
        // 创建自定义 JSON 模式
        ChatCompletionsJsonSchemaResponseFormatJsonSchema responseFormatJsonSchema = new ChatCompletionsJsonSchemaResponseFormatJsonSchema("my_answer_format");
        responseFormatJsonSchema.setStrict(true);
        // 创建 ChatCompletionsResponseFormat 并设置为 json_schema 类型和自定义 JSON 模式
        responseFormatJsonSchema.setSchema(BinaryData.fromString(jsonResponseFormat));
        return new ChatCompletionsJsonSchemaResponseFormat(responseFormatJsonSchema);
    }
}
