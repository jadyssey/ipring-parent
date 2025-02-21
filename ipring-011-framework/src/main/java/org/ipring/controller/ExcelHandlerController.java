package org.ipring.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.anno.StlApiOperation;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.model.ImportExcelVO;
import org.ipring.model.common.Return;
import org.ipring.util.JsonUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2/18/2025
 */
@Slf4j
@Api(tags = "excel处理接口")
@RequestMapping("/excel")
@RestController
@Validated
@RequiredArgsConstructor
public class ExcelHandlerController {

    /**
     * 接口请求日志的api地址匹配
     */
    private static final String apiUrlRegex = "\\[API =([^]]+)\\]";
    private static final String logTextApiUrlRegex = "请求开始 \\[API =([^\\]]+)\\]";
    private static final Pattern logTextPattern = Pattern.compile(logTextApiUrlRegex);

    @PostMapping("/import-api-url")
    @StlApiOperation(title = "请求日志提取API地址-导入数据批量调用", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void importExcelApiUrl(@RequestParam("file") MultipartFile file) {
        List<ImportExcelVO> podList = ExcelOperateUtils.importToList(file, ImportExcelVO.class);
        Pattern pattern = Pattern.compile(apiUrlRegex);
        List<String> apiUrlList = podList.stream().map(pod -> {
            Matcher matcher = pattern.matcher(pod.getMessage());
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }).collect(Collectors.toList());

        List<String> stringLongMap = this.groupAndCountWithStream(apiUrlList);
        log.info("stringLongMap = " + JsonUtils.toJson(stringLongMap));
    }

    @PostMapping("/text-apiUrl")
    @StlApiOperation(title = "从文本中提取apiUrl", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public void textApiUrl(@RequestBody String importExcelVO) {
        Matcher m = logTextPattern.matcher(importExcelVO);

        List<String> urls = new ArrayList<>();
        while (m.find()) {
            // 提取匹配到的 API 路径
            String url = m.group(1);
            // 去除可能存在的多余信息，如请求方法（POST、GET 等）
            url = url.split(" ")[0];
            urls.add(url);
        }

        urls = this.groupAndCountWithStream(urls);
        urls = filterSystemConfig(urls);
        log.info("stringLongMap = " + JsonUtils.toJson(urls));
    }


    @PostMapping("/text-api-name")
    @StlApiOperation(title = "从controller中提取apiUrl和name", subCodeType = SystemServiceCode.SystemApi.class, response = Return.class)
    public List<String> textApiName(@RequestBody String controllerText) {
        List<String> interfaceUrlList = new ArrayList<>();

        // 匹配 @PostMapping 或 @GetMapping 注解中的 URL
        Pattern mappingPattern = Pattern.compile("@(PutMapping|PostMapping|GetMapping)\\(\"([^\"]+)\"\\)");
        Matcher mappingMatcher = mappingPattern.matcher(controllerText);

        // 匹配 @ApiOperation 注解中的 value 属性
        Pattern apiOperationPattern = Pattern.compile("@ApiOperation\\([^)]*value\\s*=\\s*\"([^\"]+)\"[^)]*\\)");

        while (mappingMatcher.find()) {
            String url = mappingMatcher.group(2);
            int startIndex = mappingMatcher.end();
            Matcher apiOperationMatcher = apiOperationPattern.matcher(controllerText);
            if (apiOperationMatcher.find(startIndex)) {
                String interfaceName = apiOperationMatcher.group(1);
                interfaceUrlList.add(interfaceName + ":::" + url);
            }
        }

        log.info("接口地址：{}", JsonUtils.toJson(interfaceUrlList));
        return interfaceUrlList;
    }



    /**
     * 过滤 查询参数配置的请求url
     *
     * @param urlList
     * @return
     */
    private List<String> filterSystemConfig(List<String> urlList) {
        String configUrlApi = "/system/config/configKey/";
        return urlList.stream().filter(url -> !url.contains(configUrlApi)).collect(Collectors.toList());
    }


    // 使用 Stream API 进行分组统计
    private List<String> groupAndCountWithStream(List<String> urlList) {
        Map<String, Long> countMap = urlList.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return countMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).map(entry -> entry.getValue() + "::" + entry.getKey()).collect(Collectors.toList());

    }

}
