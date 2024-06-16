package org.ipring.camunda;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;

public class WorkflowDemo {
    public static void main(String[] args) {
        // 创建流程引擎
        ProcessEngine processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .buildProcessEngine();
        
        // 部署流程定义
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("process.bpmn")
                .deploy();
        
        // 启动流程实例
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        System.out.println("流程实例ID: " + processInstance.getId());
        System.out.println("流程定义ID: " + processInstance.getProcessDefinitionId());

    }
}
