package org.ipring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.repository.Deployment;
import org.ipring.util.ProcessUtil;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProcessUtilTest {
    public final ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();

    @Test
    public void deployByResourcePathTest() {
        // deploy
        final String deployName = "serverTask03";
        final String resourcePath = "flow/serverTask03.bpmn20.xml";
        Deployment deployment = ProcessUtil.deployByResourcePath(defaultProcessEngine, resourcePath, deployName);

        assertNotNull(deployment.getId());
        assertEquals(deployName, deployment.getName());

        List<Deployment> deployments = defaultProcessEngine.getRepositoryService().createDeploymentQuery().deploymentName(deployName).list();
    }
}
