package org.qubership.integration.platform.engine.service.deployment.processing.actions.create.after;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.MetaKey;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterRoutesCreated;

import java.util.Map;

@Slf4j
@ApplicationScoped
@OnAfterRoutesCreated
public class McpToolRegistrar extends ElementProcessingAction {
    public static final String DEPLOYMENT_ID = "deploymentId";

    @Inject
    ToolManager toolManager;

    @Inject
    @Identifier("jsonMapper")
    ObjectMapper objectMapper;

    @Override
    public boolean applicableTo(ElementProperties properties) {
        String elementType = properties.getProperties().get(CamelConstants.ChainProperties.ELEMENT_TYPE);
        ChainElementType chainElementType = ChainElementType.fromString(elementType);
        return ChainElementType.MCP_TRIGGER.equals(chainElementType);
    }

    @Override
    public void apply(CamelContext context, ElementProperties properties, DeploymentInfo deploymentInfo) {
        try {
            Map<String, String> props = properties.getProperties();
            ToolManager.ToolDefinition toolDefinition = toolManager.newTool(props.get("name"));
            toolDefinition
                    .setDescription(props.get("description"))
                    .setTitle(props.get("title"))
                    .setAnnotations(new ToolManager.ToolAnnotations(
                            props.get("title"),
                            Boolean.parseBoolean(props.get("readOnly")),
                            Boolean.parseBoolean(props.get("destructive")),
                            Boolean.parseBoolean(props.get("idempotent")),
                            Boolean.parseBoolean(props.get("openWorld"))))
                    .setMetadata(Map.of(MetaKey.of(DEPLOYMENT_ID), deploymentInfo.getDeploymentId()))
                    .setInputSchema(objectMapper.readTree(props.get("inputSchema")));
            String outputSchema = props.get("outputSchema");
            boolean hasOutputSchema = StringUtils.isNotBlank(outputSchema);
            if (hasOutputSchema) {
                toolDefinition.setOutputSchema(objectMapper.readTree(outputSchema));
            }
            toolDefinition.setHandler((arguments) -> {
                ProducerTemplate producerTemplate = context.createProducerTemplate();
                String endpointUri = "direct:" + properties.getElementId();
                Exchange result = producerTemplate.request(endpointUri, exchange ->
                        exchange.getIn().setBody(arguments.args()));
                return hasOutputSchema
                        ? ToolResponse.structuredSuccess(result.getMessage().getBody())
                        : ToolResponse.success(result.getMessage().getBody(String.class));
            });
            toolDefinition.register();
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }
}
