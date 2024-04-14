package org.ipring.controller;

import io.swagger.annotations.Api;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.util.CacheUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lgj
 * @Date 2024/4/14
 */
@Api(tags = "校验测试接口")
@RequestMapping("/validation")
@RestController
@Validated
public class ValidationController {

    @GetMapping("get")
    @StlApiOperation(title = "get", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<?> get() {
        CacheUtil.get("");
        return ReturnFactory.success();
    }
}
