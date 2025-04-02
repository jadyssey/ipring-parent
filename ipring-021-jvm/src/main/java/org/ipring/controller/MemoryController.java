package org.ipring.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(tags = "jvm")
@RequestMapping("/jvm")
@RestController
@Validated
@RequiredArgsConstructor
public class MemoryController {

    @PostMapping("/test")
    @StlApiOperation(title = "jvm 测试", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public Return<?> testJvm(@RequestParam Integer size) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("imageUrl", "httpUrl");
        for (int i = 0; i < size; i++) {
            String body = HttpRequest.post("http://localhost:8060/pod/empty?" + HttpUtil.toParams(paramMap))
                    .header("Authorization", "test")
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .timeout(10000).execute().body();
            log.info("第{}次响应结果：{}", i, body);
        }
        return ReturnFactory.success();
    }
}
