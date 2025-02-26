package org.ipring.gateway;

import org.ipring.model.common.ZtReturn;
import org.ipring.model.delivery.AmazonBatchFileVO;

import java.util.List;

/**
 * @author liuguangjin
 * @date 2025/2/11
 */
public interface DeliveryGateway {
    ZtReturn<List<String>> usBatchDownloadImg(AmazonBatchFileVO amazonBatchFileVO);
    ZtReturn<List<String>> frBatchDownloadImg(AmazonBatchFileVO amazonBatchFileVO);
}
