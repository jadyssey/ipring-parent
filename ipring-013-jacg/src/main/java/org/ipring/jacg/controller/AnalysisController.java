package org.ipring.jacg.controller;

import com.adrninistrator.jacg.common.enums.OutputDetailEnum;
import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.jacg.dto.methodcall.MethodCallLineData4Ee;
import com.adrninistrator.jacg.runner.RunnerGenAllGraph4Callee;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.anno.StlApiOperation;
import org.ipring.jacg.process.SimpleCallChainProcessor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public void similarity() {
        ConfigureWrapper configureWrapper = getConfigureWrapper();
        configureWrapper.setOtherConfigSet(OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE, "com.cds.apple.mapper.HubAssignTaskMapper:findInfoById");
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY, Boolean.TRUE.toString());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL, OutputDetailEnum.ODE_2.getDetail());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_GEN_CALL_GRAPH_DEPTH_LIMIT, "15");

        RunnerGenAllGraph4Callee runnerGenAllGraph4Callee = new RunnerGenAllGraph4Callee(configureWrapper);
        boolean run = runnerGenAllGraph4Callee.run();
        Map<String, List<MethodCallLineData4Ee>> allMethodCallLineData4EeMap = runnerGenAllGraph4Callee.getAllMethodCallLineData4EeMap();
        allMethodCallLineData4EeMap.forEach((key, list) -> {
            List<String> result = simpleCallChainProcessor.extractLeafPathsByModel(list);
            System.out.println("叶子节点的完整路径:");
            for (String chain : result) {
                System.out.println(chain);
            }
        });
        log.info("runnerGenAllGraph4Callee.run = {}", run);
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
}
