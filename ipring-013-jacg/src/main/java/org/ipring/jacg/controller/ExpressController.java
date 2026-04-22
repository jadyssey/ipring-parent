package org.ipring.jacg.controller;

import com.alibaba.fastjson.JSONObject;
import org.ipring.jacg.model.ApiResult;
import org.ipring.jacg.model.ExpressStatus;
import org.ipring.util.JsonUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/express")
public class ExpressController {

    /**
     * 接收原始JSON，解析并返回 itemCode + itemValue 列表
     */
    @PostMapping("/parse-status")
    public List<String> parseExpressStatus(@RequestBody ApiResult json) {
        List<String> resp = json.getData().getRecords().stream().sorted(Comparator.comparing(ExpressStatus::getItemCode))
                .map(es -> es.getItemCode() + "#" + es.getItemValue()).collect(Collectors.toList());
        System.out.println(" ========================================= ");
        resp.forEach(System.out::println);
        return resp;
    }

    /**
     * apifox导出接口json格式提取uri
     * @param openApiJson
     * @return
     */
    @PostMapping("/extract/uri")
    public List<String> extractUri(@RequestBody String openApiJson) {
        List<String> uriList = new ArrayList<>();

        // 解析JSON
        JSONObject jsonObject = JSONObject.parseObject(openApiJson);
        // 获取所有path
        JSONObject paths = jsonObject.getJSONObject("paths");

        if (paths != null) {
            Set<String> uris = paths.keySet();
            uriList.addAll(uris);
        }
        uriList.forEach(System.out::println);
        return uriList;
    }
}