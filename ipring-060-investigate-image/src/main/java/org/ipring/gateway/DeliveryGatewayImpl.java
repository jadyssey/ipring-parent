package org.ipring.gateway;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class DeliveryGatewayImpl implements DeliveryGateway {

    @Resource
    private USDeliveryApi usDeliveryApi;
    @Resource
    private FRDeliveryApi frDeliveryApi;

    @Override
    public ZtReturn<List<String>> usBatchDownloadImg(AmazonBatchFileVO amazonBatchFileVO) {
        String secretKey = HttpUtils.getHeader("Authorization");
        if (StringUtils.isBlank(secretKey)) {
            secretKey = amazonBatchFileVO.getAuthorization();
        }
        // 调用batchDownloadImg下载图片
        return usDeliveryApi.batchDownloadImg("Bearer " + secretKey, amazonBatchFileVO);
    }

    @Override
    public ZtReturn<List<String>> frBatchDownloadImg(AmazonBatchFileVO amazonBatchFileVO) {
        String secretKey = HttpUtils.getHeader("Authorization");
        if (StringUtils.isBlank(secretKey)) {
            secretKey = amazonBatchFileVO.getAuthorization();
        }
        // 调用batchDownloadImg下载图片
        return frDeliveryApi.batchDownloadImg("Bearer " + secretKey, amazonBatchFileVO);
    }
}
