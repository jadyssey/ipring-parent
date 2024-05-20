package org.ipring.httpclient;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import org.ipring.model.common.Return;
import org.ipring.model.httpclient.dto.OrderAddDTO;
import org.ipring.model.httpclient.response.ct4.ModifyOrderVO;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "${ipring.config.ct4}")
public interface ICt4Service {
    /**
     * 下单
     */
    @POST(value = "/trade/order")
    Return<ModifyOrderVO> makeOrder(@Header(value = "ct4Token") String ct4Token, @Body OrderAddDTO orderAddDTO);
}