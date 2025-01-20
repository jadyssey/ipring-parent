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
@RetrofitClient(baseUrl = "https://api.openai.com", connectTimeoutMs = 20 * 1000, callTimeoutMs = 20 * 1000)
public interface ChatGptApi {
    @POST(value = "/v1/chat/completions")
    ChatGPTResponse completions(@Header(value = "Authorization") String Authorization, @Body Map<String, Object> data);
}
