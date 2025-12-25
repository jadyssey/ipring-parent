package org.ipring.jacg;

import com.adrninistrator.jacg.common.enums.OutputDetailEnum;
import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.runner.RunnerWriteDb;
import com.adrninistrator.jacg.runner.base.AbstractRunner;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author liuguangjin
 * @date 2025/12/13
 **/
@Slf4j
@SpringBootApplication
public class JacgApp {

    public static void main1(String[] args) {
        SpringApplication.run(JacgApp.class, args);
    }


    public static void main(String[] args) {
        run();
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
        javaCG2ConfigureWrapper.setOtherConfigList(
                JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR,
                "D:\\git\\usCode\\dbu-mod-task\\dbu-mod-task-provider\\target\\dbu-mod-task.jar"
        );

        ConfigureWrapper configureWrapper = getConfigureWrapper();
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_OUTPUT_DETAIL,  OutputDetailEnum.ODE_2.getDetail());
        // configureWrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_GEN_STACK_OTHER_FORMS,   Boolean.TRUE.toString());

        boolean success = new RunnerWriteDb(javaCG2ConfigureWrapper, configureWrapper).run();
        System.out.println("success = " + success);
    }
    private static ConfigureWrapper getConfigureWrapper() {
        ConfigureWrapper configureWrapper = new ConfigureWrapper();
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_DB_INSERT_BATCH_SIZE, "1000");
        configureWrapper.setMainConfig(ConfigKeyEnum.CKE_THREAD_NUM, "50");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USE_H2, Boolean.FALSE.toString());
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_DRIVER_NAME, "com.mysql.cj.jdbc.Driver");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_URL, "jdbc:mysql://10.100.12.227:63307/a_jacg_task?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USERNAME, "rabee_dev");
        configureWrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_PASSWORD, "K5qHHrqF26qxmm2jLJ");
        return configureWrapper;
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
