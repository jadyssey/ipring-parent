package org.ipring.gateway;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import org.ipring.model.common.Return;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@RetrofitClient(baseUrl = "https://api.openai.com")
public interface ChatGptApi {
    @POST(value = "/v1/chat/completions")
    Return<String> completions(@Header(value = "Authorization") String Authorization, @Body ChatCompletionRequest data);
}
