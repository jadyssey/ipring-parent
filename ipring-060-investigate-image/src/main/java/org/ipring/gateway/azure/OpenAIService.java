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
        if (StringUtils.isNotBlank(chatBody.getSystemSetup())) {
            // 添加系统消息
            chatMessages.add(new ChatRequestSystemMessage(chatBody.getSystemSetup()));
        }

        // 创建包含文本和图片的用户消息列表
        List<ChatMessageContentItem> contentItems = new ArrayList<>();

        // todo 示例图片，不能使用该临时链接
        // String tempImg = "https://gofo-sys-admin.s3.us-west-2.amazonaws.com/sys-mod-file/2025-02-20/app-file/17401027601681174414C-9574-4DF1-80BE-B018B903AE80-4842-000001022BBCF857.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250221T135427Z&X-Amz-SignedHeaders=host&X-Amz-Expires=604800&X-Amz-Credential=AKIAR234HW752KIISC4O%2F20250221%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Signature=55ad76322a03f3b8c456c92f84ebb80abc2af9596dff79e9ab3908dd0b268ac0";
        // ChatMessageImageContentItem tempImageContentItem = new ChatMessageImageContentItem(new ChatMessageImageUrl(tempImg));
        // contentItems.add(tempImageContentItem);

        // 添加图片内容
        imageList.forEach(imageUrl -> {
            ChatMessageImageUrl chatMessageImageUrl = new ChatMessageImageUrl(imageUrl);
//            chatMessageImageUrl.setDetail(ChatMessageImageDetailLevel.LOW);
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
