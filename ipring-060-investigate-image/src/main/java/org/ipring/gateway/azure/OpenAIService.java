package org.ipring.gateway.azure;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import org.ipring.util.AzureAiProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {

    @Resource
    private AzureAiProperties azureAiProperties;

    public static OpenAIClient client;

    public ChatCompletions getChatResponse(String imgList, String userMessage) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant."));
        chatMessages.add(new ChatRequestUserMessage(userMessage));
        if (client == null) {
            client = new OpenAIClientBuilder()
                    .endpoint(azureAiProperties.getEndpoint())
                    .credential(new AzureKeyCredential(azureAiProperties.getApiKey()))
                    .buildClient();
        }

        return client.getChatCompletions(azureAiProperties.getModelId(), new ChatCompletionsOptions(chatMessages));
  /*
        StringBuilder response = new StringBuilder();
        for (ChatChoice choice : chatCompletions.getChoices()) {
            response.append(choice.getMessage().getContent()).append("\n");
        }
        return response.toString();*/
    }
}
