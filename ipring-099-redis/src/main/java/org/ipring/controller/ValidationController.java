package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.ipring.util.RedisUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * @Author lgj
 * @Date 2024/4/14
 */
@Api(tags = "测试接口")
@RequestMapping("/redis")
@RestController
@Validated
@RequiredArgsConstructor
public class ValidationController {
    private final RedisUtil redisUtil;

    @GetMapping("get")
    @StlApiOperation(title = "get", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<?> get() {
        String key = "test";
        redisUtil.set(key, "abc", Duration.ofSeconds(10));
        Object s = redisUtil.get(key);
        redisUtil.del(key);
        return ReturnFactory.success();
    }
}
