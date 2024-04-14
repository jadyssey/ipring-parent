package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.ipring.chain.AbstractChainContext;
import org.ipring.chain.enums.OrderCreateEnum;
import org.ipring.chain.enums.OrderUpdateEnum;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.model.param.order.OrderAddParam;
import org.ipring.model.param.order.OrderUpdateParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lgj
 * @Date 2024/4/14
 */
@RequestMapping("chain")
@RestController
@RequiredArgsConstructor
@Api(tags = "责任链模式")
public class ChainController {
    private final AbstractChainContext<Object> abstractChainContext;
    // private final AbstractChainContext<OrderUpdateParam> updateAbstractChainContext;

    @PostMapping("order")
    @StlApiOperation(title = "测试责任链过滤方案")
    public Return<?> add(@RequestBody OrderAddParam param) {
        abstractChainContext.handler(OrderCreateEnum.class, param);
        return ReturnFactory.success();
    }

    @PutMapping("order")
    @StlApiOperation(title = "测试责任链过滤方案")
    public Return<?> update(@RequestBody OrderUpdateParam param) {
        abstractChainContext.handler(OrderUpdateEnum.class, param);
        return ReturnFactory.success();
    }
}
