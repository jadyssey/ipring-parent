package org.ipring.jacg.controller;

import com.adrninistrator.jacg.common.enums.OutputDetailEnum;
import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.jacg.dto.methodcall.MethodCallLineData4Ee;
import com.adrninistrator.jacg.runner.RunnerGenAllGraph4Callee;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.ipring.jacg.process.SimpleCallChainProcessor;
import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @GetMapping("/logTest")
    public void logTest() {
        log.trace("TRACE 级别日志");
        log.debug("DEBUG 级别日志");
        log.info("INFO 级别日志");
        log.warn("WARN 级别日志");
        log.error("ERROR 级别日志");
    }

    @PostMapping("/runnerGenAllGraph4Callee")
    @StlApiOperation(title = "向上调用链")
    public Return<String> similarity(@RequestParam String excelName, @RequestParam String mapperName, @RequestParam(required = false) String depthLimit) {
        ConfigureWrapper configureWrapper = getConfigureWrapper();
        configureWrapper.setOtherConfigSet(OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE, mapperName);
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY, Boolean.TRUE.toString());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL, OutputDetailEnum.ODE_2.getDetail());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_GEN_CALL_GRAPH_DEPTH_LIMIT, Optional.ofNullable(depthLimit).filter(StringUtils::isNotBlank).orElse("15"));

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
        SXSSFWorkbook sxssfWorkbook = ExcelOperateUtils.exportToBigDataFile(calleeExcelList);
        String fileNameResp = writeLocalPath("Callee_" + excelName, sxssfWorkbook);
        log.info("runnerGenAllGraph4Callee.run = {}", run);
        if (run) {
            return ReturnFactory.success(fileNameResp);
        }
        return ReturnFactory.error();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping("/apmApi")
    @StlApiOperation(title = "apm解析")
    @SneakyThrows
    public Return<String> apmApi(@RequestBody ApmUriVO apm) {
        // 解析JSON到List<ApiMetric>
        List<ApmUriVO> metricList = OBJECT_MAPPER.readValue(apm.getName(), new TypeReference<List<ApmUriVO>>() {
        });
        System.out.println("接口地址如下");
        metricList.stream().filter(metric -> !metric.getName().contains(".")).forEach(metric -> {
            System.out.println(metric.getName().replace("SpringController", ""));
        });
        return ReturnFactory.success();
    }

    private static ConfigureWrapper getConfigureWrapper() {
        ConfigureWrapper configureWrapper = new ConfigureWrapper();
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_DB_INSERT_BATCH_SIZE, "1000");
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_THREAD_NUM, "50");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USE_H2, Boolean.FALSE.toString());
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_DRIVER_NAME, "com.mysql.cj.jdbc.Driver");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_URL, "jdbc:mysql://10.100.12.227:63307/a_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USERNAME, "rabee_dev");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_PASSWORD, "K5qHHrqF26qxmm2jLJ");
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
}
