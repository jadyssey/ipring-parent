package org.ipring.client;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import org.ipring.client.baseservice.SendEmailSubmit;
import org.ipring.model.common.Return;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@RetrofitClient(baseUrl = "${stl.config.svc.base}")
public interface IBaseService {
    /**
     * 发送自定义模板邮件
     *
     * @param sendEmailSubmit 参数
     * @return 返回对象
     */
    @POST(value = "/api/email/postSendCustomCode")
    Return<String> sendEmail(@Body SendEmailSubmit sendEmailSubmit);

    /**
     * 查询指定国家手机号正则表达式
     *
     * @param regionCode 参数
     * @return 返回对象
     */
    @GET(value = "/api/area/getByRegionCode")
    Return<String> getByRegionCode(@Query("regionCode") String regionCode);

}