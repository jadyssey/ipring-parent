package org.ipring.gateway;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import org.ipring.model.common.Return;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "https://open.bigmodel.cn/api/paas/v4")
public interface ZhiPuAiApi {
    @POST(value = "/chat/completions")
    Return<String> completions(@Header(value = "Authorization") String Authorization, @Body ChatCompletionRequest data);
}