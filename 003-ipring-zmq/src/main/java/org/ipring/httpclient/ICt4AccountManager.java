package org.ipring.httpclient;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import org.ipring.model.AccountAddParam;
import org.ipring.model.common.Return;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "${ipring.config.ct4-account}")
public interface ICt4AccountManager {
    @POST(value = "/account/create")
    Return<Object> crateAccount(@Header(value = "uid") String uid, @Body AccountAddParam accountAddParam);
}