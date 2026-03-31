package org.qubership.integration.platform.engine.service.deployment.processing.actions.delete.before;

import io.quarkiverse.mcp.server.MetaKey;
import io.quarkiverse.mcp.server.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentUpdate;
import org.qubership.integration.platform.engine.service.deployment.processing.DeploymentProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnBeforeRoutesDeleted;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.qubership.integration.platform.engine.service.deployment.processing.actions.create.after.McpToolRegistrar.DEPLOYMENT_ID;

@ApplicationScoped
@OnBeforeRoutesDeleted
public class McpToolUnregisterAction implements DeploymentProcessingAction {
    @Inject
    ToolManager toolManager;

    @Override
    public void execute(CamelContext context, DeploymentUpdate deploymentUpdate) {
        List<ToolManager.ToolInfo> toolsToRemove = StreamSupport.stream(toolManager.spliterator(), false)
                .filter(tool -> deploymentUpdate.getDeploymentInfo().getDeploymentId()
                        .equals(tool.metadata().get(MetaKey.of(DEPLOYMENT_ID))))
                .toList();
        toolsToRemove.forEach(tool -> toolManager.removeTool(tool.name()));
    }
}
