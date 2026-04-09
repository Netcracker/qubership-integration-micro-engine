package org.qubership.integration.platform.engine.service.deployment.processing.actions.create.after;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.MetaKey;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.testutils.DisplayNameUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameUtils.ReplaceCamelCase.class)
class McpToolRegistrarTest {

    private static final String DEPLOYMENT_ID = "deployment-123";
    private static final String ELEMENT_ID = "element-456";
    private static final String TOOL_NAME = "myTool";
    private static final String TOOL_TITLE = "My Tool";
    private static final String TOOL_DESCRIPTION = "A test tool";
    private static final String INPUT_SCHEMA = """
            {"type": "object", "properties": {"input": {"type": "string"}}}
            """;
    private static final String OUTPUT_SCHEMA = """
            {"type": "object", "properties": {"output": {"type": "string"}}}
            """;

    @Mock
    ToolManager toolManager;

    @Mock
    ToolManager.ToolDefinition toolDefinition;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    McpToolRegistrar registrar;

    @Test
    void applicableToShouldReturnTrueForMcpTrigger() {
        ElementProperties properties = ElementProperties.builder()
                .properties(Map.of(ChainProperties.ELEMENT_TYPE, "mcp-trigger"))
                .build();

        assertTrue(registrar.applicableTo(properties));
    }

    @Test
    void applicableToShouldReturnFalseForNonMcpTriggerElementType() {
        for (String elementType : List.of("http-trigger", "kafka-trigger", "scheduler", "script")) {
            ElementProperties properties = ElementProperties.builder()
                    .properties(Map.of(ChainProperties.ELEMENT_TYPE, elementType))
                    .build();

            assertFalse(registrar.applicableTo(properties), "Expected false for element type: " + elementType);
        }
    }

    @Test
    void applyShouldRegisterToolWithCorrectNameDescriptionAndTitle() {
        setupToolDefinitionChain();

        registrar.apply(mock(CamelContext.class), buildElementProperties(false), buildDeploymentInfo());

        verify(toolManager).newTool(TOOL_NAME);
        verify(toolDefinition).setDescription(TOOL_DESCRIPTION);
        verify(toolDefinition).setTitle(TOOL_TITLE);
    }

    @Test
    void applyShouldRegisterToolWithAnnotations() {
        setupToolDefinitionChain();

        registrar.apply(mock(CamelContext.class), buildElementProperties(false), buildDeploymentInfo());

        ArgumentCaptor<ToolManager.ToolAnnotations> annotationsCaptor =
                ArgumentCaptor.forClass(ToolManager.ToolAnnotations.class);
        verify(toolDefinition).setAnnotations(annotationsCaptor.capture());

        ToolManager.ToolAnnotations annotations = annotationsCaptor.getValue();
        assertEquals(TOOL_TITLE, annotations.title());
        assertTrue(annotations.readOnlyHint());
        assertFalse(annotations.destructiveHint());
        assertTrue(annotations.idempotentHint());
        assertFalse(annotations.openWorldHint());
    }

    @Test
    void applyShouldSetDeploymentIdInMetadata() {
        setupToolDefinitionChain();

        registrar.apply(mock(CamelContext.class), buildElementProperties(false), buildDeploymentInfo());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<MetaKey, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(toolDefinition).setMetadata(metadataCaptor.capture());

        Map<MetaKey, Object> metadata = metadataCaptor.getValue();
        assertEquals(DEPLOYMENT_ID, metadata.get(MetaKey.of(McpToolRegistrar.DEPLOYMENT_ID)));
    }

    @Test
    void applyShouldSetInputSchema() {
        setupToolDefinitionChain();

        registrar.apply(mock(CamelContext.class), buildElementProperties(false), buildDeploymentInfo());

        verify(toolDefinition).setInputSchema(any());
    }

    @Test
    void applyShouldNotSetOutputSchemaWhenAbsent() {
        setupToolDefinitionChain();

        registrar.apply(mock(CamelContext.class), buildElementProperties(false), buildDeploymentInfo());

        verify(toolDefinition, never()).setOutputSchema(any());
    }

    @Test
    void applyShouldSetOutputSchemaWhenPresent() {
        setupToolDefinitionChain();
        when(toolDefinition.setOutputSchema(any())).thenReturn(toolDefinition);

        registrar.apply(mock(CamelContext.class), buildElementProperties(true), buildDeploymentInfo());

        verify(toolDefinition).setOutputSchema(any());
    }

    @Test
    void applyShouldCallRegister() {
        setupToolDefinitionChain();

        registrar.apply(mock(CamelContext.class), buildElementProperties(false), buildDeploymentInfo());

        verify(toolDefinition).register();
    }

    @Test
    void handlerShouldReturnSuccessResponseWithStringBodyWhenNoOutputSchema() {
        setupToolDefinitionChain();
        CamelContext context = mock(CamelContext.class);
        ProducerTemplate producerTemplate = mock(ProducerTemplate.class);
        Exchange resultExchange = mock(Exchange.class);
        Message resultMessage = mock(Message.class);

        when(context.createProducerTemplate()).thenReturn(producerTemplate);
        when(producerTemplate.request(anyString(), any())).thenReturn(resultExchange);
        when(resultExchange.getMessage()).thenReturn(resultMessage);
        when(resultMessage.getBody(String.class)).thenReturn("response-body");

        registrar.apply(context, buildElementProperties(false), buildDeploymentInfo());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<ToolManager.ToolArguments, ToolResponse>> handlerCaptor =
                ArgumentCaptor.forClass(Function.class);
        verify(toolDefinition).setHandler(handlerCaptor.capture());

        ToolManager.ToolArguments arguments = mock(ToolManager.ToolArguments.class);
        ToolResponse response = handlerCaptor.getValue().apply(arguments);

        verify(producerTemplate).request(eq("direct:" + ELEMENT_ID), any());
        assertFalse(response.isError());
        assertNull(response.structuredContent());
    }

    @Test
    void handlerShouldReturnStructuredSuccessResponseWhenOutputSchemaPresent() {
        setupToolDefinitionChain();
        when(toolDefinition.setOutputSchema(any())).thenReturn(toolDefinition);
        CamelContext context = mock(CamelContext.class);
        ProducerTemplate producerTemplate = mock(ProducerTemplate.class);
        Exchange resultExchange = mock(Exchange.class);
        Message resultMessage = mock(Message.class);
        Object responseBody = Map.of("output", "value");

        when(context.createProducerTemplate()).thenReturn(producerTemplate);
        when(producerTemplate.request(anyString(), any())).thenReturn(resultExchange);
        when(resultExchange.getMessage()).thenReturn(resultMessage);
        when(resultMessage.getBody()).thenReturn(responseBody);

        registrar.apply(context, buildElementProperties(true), buildDeploymentInfo());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<ToolManager.ToolArguments, ToolResponse>> handlerCaptor =
                ArgumentCaptor.forClass(Function.class);
        verify(toolDefinition).setHandler(handlerCaptor.capture());

        ToolManager.ToolArguments arguments = mock(ToolManager.ToolArguments.class);
        ToolResponse response = handlerCaptor.getValue().apply(arguments);

        assertFalse(response.isError());
        assertEquals(responseBody, response.structuredContent());
    }

    private void setupToolDefinitionChain() {
        when(toolManager.newTool(anyString())).thenReturn(toolDefinition);
        when(toolDefinition.setDescription(any())).thenReturn(toolDefinition);
        when(toolDefinition.setTitle(any())).thenReturn(toolDefinition);
        when(toolDefinition.setAnnotations(any())).thenReturn(toolDefinition);
        when(toolDefinition.setMetadata(any())).thenReturn(toolDefinition);
        when(toolDefinition.setInputSchema(any())).thenReturn(toolDefinition);
    }

    private ElementProperties buildElementProperties(boolean withOutputSchema) {
        Map<String, String> props = new HashMap<>();
        props.put(ChainProperties.ELEMENT_TYPE, "mcp-trigger");
        props.put("name", TOOL_NAME);
        props.put("title", TOOL_TITLE);
        props.put("description", TOOL_DESCRIPTION);
        props.put("readOnly", "true");
        props.put("destructive", "false");
        props.put("idempotent", "true");
        props.put("openWorld", "false");
        props.put("inputSchema", INPUT_SCHEMA);
        if (withOutputSchema) {
            props.put("outputSchema", OUTPUT_SCHEMA);
        }
        return ElementProperties.builder()
                .elementId(ELEMENT_ID)
                .properties(props)
                .build();
    }

    private DeploymentInfo buildDeploymentInfo() {
        return DeploymentInfo.builder()
                .deploymentId(DEPLOYMENT_ID)
                .build();
    }
}
