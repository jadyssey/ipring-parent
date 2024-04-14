package org.ipring.client;

import org.ipring.client.dto.CommonDTO;
import org.ipring.client.baseservice.SendEmailSubmit;
import org.ipring.model.common.Return;

public interface BaseServiceManager {
    /**
     * 发送自定义模板邮件
     *
     * @param sendEmailSubmit 参数
     * @return 返回对象
     */
    CommonDTO<?> sendEmail(SendEmailSubmit sendEmailSubmit);


    Return<String> getByRegionCode(String regionCode);
}
