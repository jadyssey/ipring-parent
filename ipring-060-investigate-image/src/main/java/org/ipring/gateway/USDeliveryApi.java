package org.ipring.gateway;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import org.ipring.model.common.ZtReturn;
import org.ipring.model.delivery.AmazonBatchFileVO;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.List;

/**
 * @author liuguangjin
 * @date 2025/2/11
 */
@RetrofitClient(baseUrl = "https://dms.gofoexpress.com")
public interface USDeliveryApi {
    @POST(value = "/prod-api/raBee/s3/file/batchDownload")
    ZtReturn<List<String>> batchDownloadImg(@Header(value = "Authorization") String Authorization, @Body AmazonBatchFileVO amazonBatchFileVO);
}
