package org.ipring;

import org.activiti.engine.*;
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
