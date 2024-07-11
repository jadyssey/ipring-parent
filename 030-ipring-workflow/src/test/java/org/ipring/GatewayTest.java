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

public class GatewayTest {
    public final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();

    @Test
    public void createEventListenerTest() {
        Map<String, Object> map = new HashMap<>();
        //  发起流程
        defaultProcessEngine.getRuntimeService()
                .startProcessInstanceByKey("gateway-02");
        defaultProcessEngine.getRuntimeService()
                .startProcessInstanceByKey("gateway-02");


        TaskService taskService = defaultProcessEngine.getTaskService();
        // 人事审批
        List<Task> taskOne = taskService.createTaskQuery()
                .taskAssignee("人事").list();
        assertNotNull(taskOne);
        for (int i = 0; i < taskOne.size(); i++) {
            map.put("day", i + 2);
            taskService.complete(taskOne.get(i).getId(), map);
        }

        // 经理审批
        List<Task> taskTwo = taskService.createTaskQuery()
                .taskAssignee("经理").list();
        taskTwo.forEach(task -> taskService.complete(task.getId(), map));

        // 总经理审批
        List<Task> taskThree = taskService.createTaskQuery()
                .taskAssignee("总经理").list();
        taskThree.forEach(task -> taskService.complete(task.getId(), map));




    }
}
