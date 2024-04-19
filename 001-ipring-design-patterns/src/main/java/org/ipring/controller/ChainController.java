package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.ipring.behavioral.chain.AbstractChainContext;
import org.ipring.behavioral.chain2.group.ValidationHandler;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lgj
 * @Date 2024/4/14
 */
@RequestMapping("/chain")
@RestController
@RequiredArgsConstructor
@Api(tags = "责任链模式")
public class ChainController {
    private final AbstractChainContext<Object> abstractChainContext;
    // private final AbstractChainContext<OrderUpdateParam> updateAbstractChainContext;
    private final ValidationHandler<String> orderCreateChain;

    @PostMapping("/two")
    @StlApiOperation(title = "测试责任链过滤方案2")
    public Return<?> chain2(@RequestBody String param) {
        orderCreateChain.handleReq(param);
        return ReturnFactory.success();
    }
}
