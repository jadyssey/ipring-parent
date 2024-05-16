package org.ipring.client;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import org.ipring.client.dto.OrderAddDTO;
import org.ipring.client.response.ct4.ModifyOrderVO;
import org.ipring.model.common.Return;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "${ipring.config.svc.ct4}")
public interface ICt4Service {
    /**
     * 下单
     */
    @POST(value = "/trade/order")
    Return<ModifyOrderVO> makeOrder(@Header(value = "ct4Token") String ct4Token, @Body OrderAddDTO orderAddDTO);
}