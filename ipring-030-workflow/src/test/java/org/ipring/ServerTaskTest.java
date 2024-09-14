package org.ipring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// @SpringBootTest
// @ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
//                 classes = {SecurityAutoConfiguration.class}))
// @RunWith(SpringRunner.class)
public class ServerTaskTest {
    public final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();

    @Test
    public void createTest01() {
        //  发起流程
        ProcessInstance servertask = defaultProcessEngine.getRuntimeService().startProcessInstanceByKey("servertask01");


        TaskService taskService = defaultProcessEngine.getTaskService();
        // 张三
        List<Task> taskOne = taskService.createTaskQuery().taskAssignee("张三").list();
        assertNotNull(taskOne);
        taskOne.forEach(task -> taskService.complete(task.getId()));

        // 执行系统任务

        // 李四
        List<Task> taskTwo = taskService.createTaskQuery().taskAssignee("李四").list();
        taskTwo.forEach(task -> taskService.complete(task.getId()));
    }

    /**
     * 系统任务，
     */
    @Test
    public void createTest02() {
        //  发起流程
        ProcessInstance servertask = defaultProcessEngine.getRuntimeService().startProcessInstanceByKey("serverTask02");
        // 执行系统任务
    }

    /**
     * 系统任务，自定义Bean
     */
    @Test
    public void completeTask1() {
        ProcessInstance pi = defaultProcessEngine.getRuntimeService().startProcessInstanceByKey("serverTask03");
        TaskService taskService = defaultProcessEngine.getTaskService();
        Task task = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();

        Map<String, Object> vars = new HashMap<>();
        vars.put("key1", "aaa");
        vars.put("key2", "bbb");
        vars.put("hname", "中国");
        String taskId = task.getId();
        taskService.complete(taskId, vars);
    }
}
