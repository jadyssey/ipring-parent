package org.ipring;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.junit.Before;
import org.junit.Rule;

public class AbstractTest {
    @Rule
    public final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();

    protected ProcessEngine processEngine;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;
    protected HistoryService historyService;
    protected ManagementService managementService;

    @Before
    public void init() {
        processEngine = defaultProcessEngine;
        repositoryService = defaultProcessEngine.getRepositoryService();
        runtimeService = defaultProcessEngine.getRuntimeService();
        taskService = defaultProcessEngine.getTaskService();
        historyService = defaultProcessEngine.getHistoryService();
        managementService = defaultProcessEngine.getManagementService();
    }
}
