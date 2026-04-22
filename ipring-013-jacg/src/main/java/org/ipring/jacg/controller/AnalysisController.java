package org.ipring.jacg.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.adrninistrator.jacg.common.enums.OutputDetailEnum;
import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.jacg.dto.methodcall.MethodCallLineData4Ee;
import com.adrninistrator.jacg.el.enums.ElConfigEnum;
import com.adrninistrator.jacg.runner.RunnerGenAllGraph4Callee;
import com.adrninistrator.jacg.runner.RunnerWriteDb;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.el.enums.CommonElAllowedVariableEnum;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.ipring.anno.StlApiOperation;
import org.ipring.excel.ExcelOperateUtils;
import org.ipring.jacg.model.ApmUriVO;
import org.ipring.jacg.model.CalleeExcelVO;
import org.ipring.jacg.model.MetricResponse;
import org.ipring.jacg.model.ResponseDTO;
import org.ipring.jacg.process.SimpleCallChainProcessor;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2025/12/23
 **/

@Api(tags = "测试接口")
@RequestMapping("/analysis")
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final SimpleCallChainProcessor simpleCallChainProcessor;

    @PostMapping("/runnerGenAllGraph4Callee")
    @StlApiOperation(title = "向上调用链")
    public Return<String> similarity(@RequestParam String dbName, @RequestParam String excelName, @RequestBody Set<String> mapperName, @RequestParam(required = false) String depthLimit) {
        ConfigureWrapper configureWrapper = getConfigureWrapper(dbName);
        if (CollectionUtil.isNotEmpty(mapperName)) {
            configureWrapper.setOtherConfigSet(OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE, mapperName);
        }
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY, Boolean.TRUE.toString());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL, OutputDetailEnum.ODE_2.getDetail());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_GEN_CALL_GRAPH_DEPTH_LIMIT, Optional.ofNullable(depthLimit).filter(StringUtils::isNotBlank).orElse("15"));
        // configureWrapper.setElConfigText(ElConfigEnum.ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL, "ee_package_name != 'com.cds'");

        RunnerGenAllGraph4Callee runnerGenAllGraph4Callee = new RunnerGenAllGraph4Callee(configureWrapper);
        boolean run = runnerGenAllGraph4Callee.run();
        Map<String, List<MethodCallLineData4Ee>> allMethodCallLineData4EeMap = runnerGenAllGraph4Callee.getAllMethodCallLineData4EeMap();
        List<CalleeExcelVO> calleeExcelList = new ArrayList<>();
        allMethodCallLineData4EeMap.forEach((key, list) -> {
            List<String> result = simpleCallChainProcessor.extractLeafPathsByModel(list);
            for (String chain : result) {
                calleeExcelList.add(CalleeExcelVO.of(chain));
            }
        });
        if (CollectionUtil.isEmpty(calleeExcelList)) return ReturnFactory.error();
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(calleeExcelList);
        String fileNameResp = writeLocalPath("Callee_" + dbName + "&" + excelName, sxssfWorkbook);
        log.info("runnerGenAllGraph4Callee.run = {}", run);
        if (run) {
            return ReturnFactory.success(fileNameResp);
        }
        return ReturnFactory.error();
    }

    @PostMapping("/runJar")
    @StlApiOperation(title = "分析jar包初始化到数据库")
    public Return<String> runJar(@RequestParam String dbName, @RequestParam(required = false) String jarPath) {
        run();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        javaCG2ConfigureWrapper.setOtherConfigList(
                JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR,
                Optional.ofNullable(jarPath).orElse("D:\\git\\usCode\\dbu-mod-waybill\\dbu-mod-waybill-provider\\target\\dbu-mod-waybill.jar")
        );

        ConfigureWrapper configureWrapper = getConfigureWrapper(dbName);
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL, OutputDetailEnum.ODE_2.getDetail());
        // configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_GEN_STACK_OTHER_FORMS,   Boolean.TRUE.toString());

        boolean success = new RunnerWriteDb(javaCG2ConfigureWrapper, configureWrapper).run();
        System.out.println("success = " + success);
        return ReturnFactory.success();
    }

    @PostMapping("/apmApi")
    @StlApiOperation(title = "apm解析")
    @SneakyThrows
    public Return<String> apmApi(@RequestBody List<ApmUriVO> apm) {
        // 解析JSON到List<ApiMetric>
        long now = System.currentTimeMillis();
        log.info("{} apm 解析接口如下", now);
        apm.stream()
                .filter(metric -> !metric.getName().contains(".") && !metric.getName().startsWith("URI") && !metric.getName().startsWith("NormalizedUri"))
                .forEach(metric -> {
                    String name = metric.getName().replace("SpringController", "").replace(" (POST)", "").replace(" (PUT)", "").replace(" (DELETE)", "");
                    System.out.println(name);
                });
        log.info("{} apm 解析接口如下", now);
        return ReturnFactory.success();
    }

    @PostMapping("/apmDatabase")
    @StlApiOperation(title = "apm数据表响应解析")
    public Return<String> apmDatabase(@RequestBody List<ApmUriVO> apm) {
        // 解析JSON到List<ApiMetric>
        long now = System.currentTimeMillis();
        log.info("{} apm 解析如下", now);
        apm.forEach(metric -> System.out.println(metric.getName()));
        log.info("{} apm 解析如下", now);
        return ReturnFactory.success();
    }

    /**
     * 解析接口，只返回 topic 名称列表
     */
    @PostMapping("/rocketmq/parse/topics")
    @StlApiOperation(title = "解析rocketMq Consumer消费组订阅topic接口返回模型 /consumer/consumerConnection.query")
    public List<String> parseTopics(@RequestBody String jsonResponse) {
        try {
            // 1. 解析 JSON
            ResponseDTO response = JSONUtil.toBean(jsonResponse, ResponseDTO.class);
            // 2. 提取所有 topic 名称
            List<String> topicNameList = response.getData().getSubscriptionTable().values().stream()
                    .map(ResponseDTO.SubscriptionDTO::getTopic)
                    .collect(Collectors.toList());
            log.info("解析结果如下：");
            topicNameList.forEach(System.out::println);
            log.info("解析结果如上：");
            return topicNameList;
        } catch (Exception e) {
            throw new RuntimeException("解析失败：" + e.getMessage());
        }
    }

    /**
     * 提取所有 topic 名称
     */
    @PostMapping("/grafana/topic")
    public List<String> extractTopics(@RequestBody String json) {
        try {
            // 解析JSON
            MetricResponse response = JSONUtil.toBean(json, MetricResponse.class);

            // 提取所有topic
            List<String> topicNameList = response.getData().getResult().stream()
                    .filter(resultDTO -> !"0".equals(resultDTO.getValue().get(1)))
                    .map(MetricResponse.ResultDTO::getMetric)
                    .map(MetricResponse.MetricDTO::getTopic)
                    .collect(Collectors.toList());
            log.info("解析结果如下：");
            topicNameList.forEach(System.out::println);
            log.info("解析结果如上：");
            return topicNameList;
        } catch (Exception e) {
            throw new RuntimeException("解析失败：" + e.getMessage());
        }
    }


    public static ConfigureWrapper getConfigureWrapper(String dbName) {
        ConfigureWrapper configureWrapper = new ConfigureWrapper();
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_DB_INSERT_BATCH_SIZE, "1000");
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_THREAD_NUM, "50");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USE_H2, Boolean.FALSE.toString());
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_DRIVER_NAME, "com.mysql.cj.jdbc.Driver");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_URL, "jdbc:mysql://10.100.12.227:63307/" + dbName + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USERNAME, "rabee_dev");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_PASSWORD, "K5qHHrqF26qxmm2jLJ");
        // 排除非项目包路径的调用分析
        /*configureWrapper.setElConfigText(
                ElConfigEnum.ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL,
                "!string.startsWith(" + CommonElAllowedVariableEnum.EAVE_MC_EE_PACKAGE_NAME.getVariableName() + ", 'com.cds')" + " && !string.startsWith(" + CommonElAllowedVariableEnum.EAVE_MC_EE_PACKAGE_NAME.getVariableName() + ", 'com.zt')"
        );*/
        return configureWrapper;
    }

    public static String writeLocalPath(String namePrefix, SXSSFWorkbook workbook) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-ss");
        String currentTime = dtf.format(LocalDateTime.now());
        String name = namePrefix + currentTime;
        try (FileOutputStream outputStream = new FileOutputStream(name + ".xlsx")) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭 SXSSFWorkbook，释放资源
            try {
                workbook.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return name;
    }

    public static void run() {
        // 必须调用具体的日志方法，且级别≥配置的Root级别（如DEBUG）
        log.trace("TRACE 级别日志"); // 仅 com.example.Main 会输出（级别为 TRACE）
        log.debug("DEBUG 级别日志"); // com.example 包下会输出（级别为 DEBUG）
        log.info("INFO 级别日志");   // 全局输出（根日志为 INFO）
        log.warn("WARN 级别日志");
        log.error("ERROR 级别日志");
    }

}
