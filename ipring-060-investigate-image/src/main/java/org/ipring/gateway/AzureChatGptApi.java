package org.ipring.gateway;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import org.ipring.model.gpt.ChatGPTResponse;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.Map;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@RetrofitClient(baseUrl = "https://dbu-cps-openai-test.openai.azure.com")
public interface AzureChatGptApi {

    @POST(value = "openai/deployments/gpt-4o/chat/completions?api-version=2024-08-01-preview")
    ChatGPTResponse azureGpt4o(@Header(value = "api-key") String apiKey, @Body Map<String, Object> data);

    @POST(value = "/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-08-01-preview")
    ChatGPTResponse azureGpt4oMini(@Header(value = "api-key") String apiKey, @Body Map<String, Object> data);
}
