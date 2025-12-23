package org.ipring.jacg;

import com.adrninistrator.jacg.common.enums.OutputDetailEnum;
import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.jacg.dto.methodcall.MethodCallLineData4Ee;
import com.adrninistrator.jacg.runner.RunnerGenAllGraph4Callee;
import com.adrninistrator.jacg.runner.RunnerWriteDb;
import com.adrninistrator.jacg.runner.base.AbstractRunner;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import org.ipring.jacg.process.CallChainProcessor;
import org.ipring.jacg.process.SimpleCallChainProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

/**
 * @author liuguangjin
 * @date 2025/12/13
 **/
@SpringBootApplication
public class JacgApp {

    public static void main(String[] args) {
        SpringApplication.run(JacgApp.class, args);
    }

    private static final Logger logger = LoggerFactory.getLogger(AbstractRunner.class);

    public static void main1(String[] args) {
        run();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        javaCG2ConfigureWrapper.setOtherConfigList(
                JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR,
                "D:\\git\\usCode\\dbu-mod-delivery\\cirro-admin\\target\\dbu-mod-delivery.jar"
        );

        ConfigureWrapper configureWrapper = getConfigureWrapper();
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL,  OutputDetailEnum.ODE_2.getDetail());
        // configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_GEN_STACK_OTHER_FORMS,   Boolean.TRUE.toString());

        boolean success = new RunnerWriteDb(javaCG2ConfigureWrapper, configureWrapper).run();
        System.out.println("success = " + success);
    }


   /* public static void main2(String[] args) {
        run();
        ConfigureWrapper configureWrapper = getConfigureWrapper();
        configureWrapper.setOtherConfigSet(OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE, "com.cds.apple.mapper.HubAssignTaskMapper:findInfoById");
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY,  Boolean.TRUE.toString());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL,  OutputDetailEnum.ODE_2.getDetail());
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_GEN_CALL_GRAPH_DEPTH_LIMIT,   "15");

        RunnerGenAllGraph4Callee runnerGenAllGraph4Callee = new RunnerGenAllGraph4Callee(configureWrapper);
        boolean run = runnerGenAllGraph4Callee.run();
        Map<String, List<MethodCallLineData4Ee>> allMethodCallLineData4EeMap = runnerGenAllGraph4Callee.getAllMethodCallLineData4EeMap();
        allMethodCallLineData4EeMap.forEach((key, list) -> {
            List<String> result = SimpleCallChainProcessor.extractLeafPathsByModel(list);
            System.out.println("叶子节点的完整路径:");
            for (String chain : result) {
                System.out.println(chain);
            }
        });
        logger.info("runnerGenAllGraph4Callee.run = {}", run);
    }*/

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

    public static void run() {
        // 必须调用具体的日志方法，且级别≥配置的Root级别（如DEBUG）
        logger.debug("DEBUG级日志（测试）");
        logger.info("INFO级日志（业务）");
        logger.error("ERROR级日志（异常）");
    }
}
