package org.qubership.integration.platform.engine.service.deployment.processing.actions.delete.before;

import io.quarkiverse.mcp.server.MetaKey;
import io.quarkiverse.mcp.server.ToolManager;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentUpdate;
import org.qubership.integration.platform.engine.service.deployment.processing.actions.create.after.McpToolRegistrar;
import org.qubership.integration.platform.engine.testutils.DisplayNameUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameUtils.ReplaceCamelCase.class)
class McpToolUnregisterActionTest {

    private static final String DEPLOYMENT_ID = "deployment-123";
    private static final String OTHER_DEPLOYMENT_ID = "deployment-999";
    private static final MetaKey DEPLOYMENT_ID_KEY = MetaKey.of(McpToolRegistrar.DEPLOYMENT_ID);

    @Mock
    ToolManager toolManager;

    @InjectMocks
    McpToolUnregisterAction action;

    @Test
    void executeShouldRemoveToolsBelongingToDeployment() {
        ToolManager.ToolInfo matchingTool = matchingToolInfo("tool-a", DEPLOYMENT_ID);
        ToolManager.ToolInfo otherTool = nonMatchingToolInfo(OTHER_DEPLOYMENT_ID);

        when(toolManager.spliterator()).thenReturn(List.of(matchingTool, otherTool).spliterator());

        action.execute(mock(CamelContext.class), deploymentUpdate(DEPLOYMENT_ID));

        verify(toolManager).removeTool("tool-a");
        verify(toolManager, never()).removeTool(OTHER_DEPLOYMENT_ID);
    }

    @Test
    void executeShouldNotRemoveAnyToolWhenNoneMatchDeploymentId() {
        ToolManager.ToolInfo otherTool = nonMatchingToolInfo(OTHER_DEPLOYMENT_ID);

        when(toolManager.spliterator()).thenReturn(List.of(otherTool).spliterator());

        action.execute(mock(CamelContext.class), deploymentUpdate(DEPLOYMENT_ID));

        verify(toolManager, never()).removeTool(anyString());
    }

    @Test
    void executeShouldHandleEmptyToolList() {
        when(toolManager.spliterator()).thenReturn(List.<ToolManager.ToolInfo>of().spliterator());

        action.execute(mock(CamelContext.class), deploymentUpdate(DEPLOYMENT_ID));

        verify(toolManager, never()).removeTool(anyString());
    }

    @Test
    void executeShouldRemoveAllMatchingToolsWhenMultipleBelongToDeployment() {
        ToolManager.ToolInfo tool1 = matchingToolInfo("tool-1", DEPLOYMENT_ID);
        ToolManager.ToolInfo tool2 = matchingToolInfo("tool-2", DEPLOYMENT_ID);
        ToolManager.ToolInfo tool3 = nonMatchingToolInfo(OTHER_DEPLOYMENT_ID);

        when(toolManager.spliterator()).thenReturn(List.of(tool1, tool2, tool3).spliterator());

        action.execute(mock(CamelContext.class), deploymentUpdate(DEPLOYMENT_ID));

        verify(toolManager).removeTool("tool-1");
        verify(toolManager).removeTool("tool-2");
        verify(toolManager, never()).removeTool(OTHER_DEPLOYMENT_ID);
    }

    private ToolManager.ToolInfo matchingToolInfo(String toolName, String deploymentId) {
        ToolManager.ToolInfo toolInfo = mock(ToolManager.ToolInfo.class);
        when(toolInfo.metadata()).thenReturn(Map.of(DEPLOYMENT_ID_KEY, deploymentId));
        when(toolInfo.name()).thenReturn(toolName);
        return toolInfo;
    }

    private ToolManager.ToolInfo nonMatchingToolInfo(String deploymentId) {
        ToolManager.ToolInfo toolInfo = mock(ToolManager.ToolInfo.class);
        when(toolInfo.metadata()).thenReturn(Map.of(DEPLOYMENT_ID_KEY, deploymentId));
        return toolInfo;
    }

    private DeploymentUpdate deploymentUpdate(String deploymentId) {
        return DeploymentUpdate.builder()
                .deploymentInfo(DeploymentInfo.builder()
                        .deploymentId(deploymentId)
                        .build())
                .build();
    }
}
