package org.ipring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @Author lgj
 * @Date 2024/6/29
 */
public class ActivitiOneTest {
    @Test
    public void testOne() {
        // activiti.cfg.xml配置文件
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        System.out.println(defaultProcessEngine);
    }

    @Test
    public void testTwo() {
        ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
                .setJdbcDriver("com.mysql.cj.jdbc.Driver")
                .setJdbcUrl("jdbc:mysql://192.168.136.202:3306/activiti7?createDatabaseIfNotExist=true")
                .setJdbcUsername("root")
                .setJdbcPassword("Root@123")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .buildProcessEngine();

        System.out.println(processEngine);
    }

    /**
     * 流程部署操作
     */
    @Test
    public void testThree() {
        // 1 获取processEngine对象
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        // 2 完成流程的部署操作
        RepositoryService repositoryService = defaultProcessEngine.getRepositoryService();
        // 3 完成部署操作
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("flow/test1.bpmn20.xml")
                .name("第一个审批流程")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());

    }

    /**
     * 查询流程部署和流程定义
     */
    @Test
    public void testFour() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = defaultProcessEngine.getRepositoryService();

        // 查询有哪些部署的流程 -> 查询相关流程定义信息
        // repositoryService.createDeploymentQuery() 查询流程部署相关信息
        List<Deployment> list = repositoryService.createDeploymentQuery().list();
        list.forEach(deployment -> {
            System.out.println((deployment.getId() + "---" + deployment.getName()));
        });

        // repositoryService.createProcessDefinitionQuery() 查询部署的流程的相关的定义
        List<ProcessDefinition> listDefinition = repositoryService.createProcessDefinitionQuery().list();
        listDefinition.forEach(definition -> {
            System.out.println(definition.getId()
                    .concat("---")
                    .concat(definition.getName())
                    .concat("---")
                    .concat(definition.getDescription()));
        });
    }

    /**
     * 发起一个流程
     * 发起之后，在对应的act_ru_task中就有一条对应的待办记录
     */
    @Test
    public void testFive() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = defaultProcessEngine.getRuntimeService();
        //  通过流程定义id启动流程 返回流程实例对象  （类比 类和对象 的关系）
        ProcessInstance processInstance = runtimeService.startProcessInstanceById("test1:1:3");
        System.out.println("processInstance.getId() = " + processInstance.getId());
        System.out.println("processInstance.getDeploymentId() = " + processInstance.getDeploymentId());
        System.out.println("processInstance.getDescription() = " + processInstance.getDescription());
    }

    /**
     * 查询待办
     * act_ru_task表
     */
    @Test
    public void testSix() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = defaultProcessEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().taskAssignee("张三").list();
        list.forEach(task -> {
            System.out.println("task.getId() = " + task.getId());
            System.out.println("task.getName() = " + task.getName());
            System.out.println("task.getAssignee() = " + task.getAssignee());

        });
    }

    /**
     * 任务审批
     */
    @Test
    public void testSeven() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = defaultProcessEngine.getTaskService();

        List<Task> list = taskService.createTaskQuery().taskAssignee("李四").list();

        // 根据 taskId 完成审批
        list.forEach(task -> taskService.complete(task.getId()));
    }
}










