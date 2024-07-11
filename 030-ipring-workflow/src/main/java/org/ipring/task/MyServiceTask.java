package org.ipring.task;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;


/**
 * 使用委托表达式，UEL表达式中myServiceTask必须是流程引擎(流程变量)或spring容器中的bean的名称，
 * 且这个bean必须实现 JavaDelegate接口（实现其中execute方法）
 */
public class MyServiceTask implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("MyTaskService开始执行, 自动任务：自动执行一些用户自定义的逻辑");

        String id = execution.getId();
        String processInstanceId = execution.getProcessInstanceId();
        String businessKey = execution.getProcessInstanceBusinessKey();
        String eventName = execution.getEventName();
        String processDefinitionId = execution.getProcessDefinitionId();

        // String key1 = execution.getVariable("text1", String.class);
        // System.out.println("text1:" + key1);

        System.out.println("执行实例id：" + id);
        System.out.println("流程实例id：" + processInstanceId);
        System.out.println("流程实例businessKey：" + businessKey);
        System.out.println("当前活动名称：" + eventName);
        System.out.println("流程定义id：" + processDefinitionId);
    }
}