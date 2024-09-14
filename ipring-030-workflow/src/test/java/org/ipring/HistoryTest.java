package org.ipring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @Author lgj
 * @Date 2024/7/11
 */
public class HistoryTest {
    private final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
    /**
     * 历史活动查询
     */
    @Test
    public void historyActInstanceList(){
        List<HistoricActivityInstance> list = defaultProcessEngine.getHistoryService() // 历史相关Service
                .createHistoricActivityInstanceQuery() // 创建历史活动实例查询
                .processInstanceId("25001") // 执行流程实例id
                .finished()
                .list();
        for(HistoricActivityInstance hai:list){
            System.out.println("活动ID:"+hai.getId());
            System.out.println("流程实例ID:"+hai.getProcessInstanceId());
            System.out.println("活动名称："+hai.getActivityName());
            System.out.println("办理人："+hai.getAssignee());
            System.out.println("开始时间："+hai.getStartTime());
            System.out.println("结束时间："+hai.getEndTime());
            System.out.println("=================================");
        }
    }

    @Test
    public void findCompleteTask() {
        // 查询
        List<HistoricTaskInstance> taskList = defaultProcessEngine.getHistoryService().createHistoricTaskInstanceQuery()
                .taskAssignee("张三") // 办理人
                .includeProcessVariables()
                .orderByTaskCreateTime()
                .desc() // 任务创建时间降序排列
                .finished()
                .list();
        for (HistoricTaskInstance task : taskList) {
            System.out.print(" 任务ID: " + task.getId());
            System.out.print(" ,任务名称: " + task.getName());
            System.out.print(" ,任务开始时间: " + task.getStartTime());
            System.out.print(" ,任务结束时间: " + task.getEndTime());
            System.out.print(" ,办理人: " + task.getAssignee());
            System.out.print(" ,流程定义id: " + task.getProcessDefinitionId());
            System.out.print(" ,流程实例id: " + task.getProcessInstanceId());
            System.out.println("，流程变量：" + task.getProcessVariables());
        }

    }
}
