package org.ipring.chain.filter;

import org.ipring.chain.AbstractChainHandler;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.model.param.order.OrderAddParam;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 订单创建参数必填检验
 *
 * @author chen.ma
 * @github <a href="https://github.com/opengoofy" />
 * @公众号 马丁玩编程，关注回复：资料，领取后端技术专家成长手册
 */
@Component
public final class NotNullChainHandler implements AbstractChainHandler<Object> {

    @Override
    public void handler(Object requestParam) {
        System.out.println("NotNullChainHandler run success");
        if (Objects.isNull(requestParam)) throw new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR);
    }
}
