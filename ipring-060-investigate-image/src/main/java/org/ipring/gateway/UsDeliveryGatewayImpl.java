package org.ipring.gateway;

import lombok.extern.slf4j.Slf4j;
import org.ipring.model.common.ZtReturn;
import org.ipring.model.delivery.AmazonBatchFileVO;
import org.ipring.util.HttpUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author liuguangjin
 * @date 2025/2/11
 */
@Component
@Slf4j
public class UsDeliveryGatewayImpl implements DeliveryGateway {

    @Resource
    private USDeliveryApi usDeliveryApi;

    @Override
    public ZtReturn<List<String>> batchDownloadImg(AmazonBatchFileVO amazonBatchFileVO) {
        String secretKey = HttpUtils.getHeader("Authorization");
        // 调用batchDownloadImg下载图片
        return usDeliveryApi.batchDownloadImg("Bearer " + secretKey, amazonBatchFileVO);
    }
}
