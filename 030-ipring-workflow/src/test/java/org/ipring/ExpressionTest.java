package org.ipring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @Author lgj
 * @Date 2024/6/29
 */
public class ExpressionTest {
    public final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();

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
                .addClasspathResource("flow/test2.bpmn20.xml")
                .name("第二个审批流程")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());

    }

    @Test
    public void simpleLeave02ValueExpressionTest() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = defaultProcessEngine.getRuntimeService();

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("userName1", "zhangsan");
        ProcessInstance instance = runtimeService
                .startProcessInstanceById("test1:3:15003", variables);

        assertNotNull(instance.getId());

        TaskService taskService = defaultProcessEngine.getTaskService();
        List<Task> zhangsanTasks = taskService.createTaskQuery()
                .taskAssignee("zhangsan")
                .list().stream()
                .filter(task -> instance.getId().equals(task.getProcessInstanceId()))
                .collect(Collectors.toList());
        assertEquals(1, zhangsanTasks.size());

        variables.put("userName2", "lisi");
        runtimeService.setVariables(zhangsanTasks.get(0).getExecutionId(), variables);
        taskService.complete(zhangsanTasks.get(0).getId());

        runtimeService.setVariables(zhangsanTasks.get(0).getExecutionId(), variables);
        List<Task> lisiTasks = taskService.createTaskQuery()
                .taskAssignee("lisi")
                .list().stream()
                .filter(task -> instance.getId().equals(task.getProcessInstanceId()))
                .collect(Collectors.toList());

        assertEquals(1, lisiTasks.size());
        taskService.complete(lisiTasks.get(0).getId());
    }

    @Test
    public void simpleLeave03MethodExpressionTest() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("taskService", new org.ipring.service.TaskService());
        ProcessInstance processInstance = defaultProcessEngine.getRuntimeService().startProcessInstanceByKey("simple-leave03", variables);

        TaskService taskService = defaultProcessEngine.getTaskService();
        // 人事审批
        Task task = taskService.createTaskQuery()
                .taskAssignee(new org.ipring.service.TaskService().getAssignee()).singleResult();
        assertNotNull(task);
        assertEquals(processInstance.getId(), task.getProcessInstanceId());

        // complete
        taskService.complete(task.getId());

        // 经理审批
        task = taskService.createTaskQuery()
                .taskAssignee(new org.ipring.service.TaskService().getAssignee()).singleResult();
        assertNotNull(task);
        assertEquals(processInstance.getId(), task.getProcessInstanceId());

        // complete
        taskService.complete(task.getId());
    }
}










