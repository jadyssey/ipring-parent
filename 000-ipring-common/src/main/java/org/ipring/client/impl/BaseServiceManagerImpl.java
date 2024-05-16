package org.ipring.client.impl;


import lombok.RequiredArgsConstructor;
import org.ipring.client.BaseServiceManager;
import org.ipring.client.IBaseService;
import org.ipring.client.baseservice.SendEmailSubmit;
import org.ipring.client.dto.CommonDTO;
import org.ipring.client.response.base.BaseServiceResponse;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.util.JsonUtils;
import org.ipring.util.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BaseServiceManagerImpl implements BaseServiceManager {

    private final IBaseService baseService;

    public static final Logger log = LoggerFactory.getLogger(BaseServiceManagerImpl.class);

    @Override
    public CommonDTO<?> sendEmail(SendEmailSubmit sendEmailSubmit) {
        Return<String> responseStr = baseService.sendEmail(sendEmailSubmit);
        BaseServiceResponse response = JsonUtils.toObject(responseStr, BaseServiceResponse.class);
        if (!BaseServiceResponse.check(response)) {
            if (null == response) {
                return CommonDTO.error(MessageUtils.getMsg("account.send.email.fail"));
            }
            log.info("发送邮件失败 ----- {}", response);
            return CommonDTO.error(response.getMessage());
        }
        return CommonDTO.success();
    }

    /**
     * 查询指定国家手机号正则表达式
     *
     * @param regionCode 参数
     * @return 返回通用对象
     */
    @Override
    public Return<String> getByRegionCode(String regionCode) {
        Return<String> resp = baseService.getByRegionCode(regionCode);
        if (!ReturnFactory.check(resp)) {
            log.info("查询正则表达式失败 ----- {}", resp);
            return ReturnFactory.info(SystemServiceCode.SystemApi.PARAM_ERROR);
        }
        return Optional.ofNullable(resp.getBodyMessage())
                .filter(StringUtils::hasText)
                .map(JsonUtils::toMap)
                .map(map -> map.get("regular"))
                .map(String::valueOf)
                .map(ReturnFactory::success)
                .orElseGet(() -> ReturnFactory.info(SystemServiceCode.SystemApi.PARAM_ERROR));
    }


}
