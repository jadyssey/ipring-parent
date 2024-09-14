package org.ipring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ListenerTest {
    public final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();

    @Test
    public void createEventListenerTest() {

        ProcessInstance processInstance = defaultProcessEngine.getRuntimeService()
                .startProcessInstanceByKey("simple-leave04");

        Task mazi = defaultProcessEngine.getTaskService().createTaskQuery()
                .taskAssignee("assignee-listener")
                .processInstanceId(processInstance.getId()).singleResult();
        
        assertNotNull(mazi);
        //删除流程
        defaultProcessEngine.getRuntimeService().deleteProcessInstance(processInstance.getId(), "clean data");
    }
}
