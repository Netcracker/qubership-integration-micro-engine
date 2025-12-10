package org.qubership.integration.platform.engine.state;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.k.SourceDefinition;
import org.qubership.integration.platform.engine.errorhandling.errorcode.ErrorCode;
import org.qubership.integration.platform.engine.metadata.ChainInfo;
import org.qubership.integration.platform.engine.model.engine.DeploymentInfo;
import org.qubership.integration.platform.engine.model.engine.DeploymentStatus;
import org.qubership.integration.platform.engine.model.engine.EngineDeployment;
import org.qubership.integration.platform.engine.model.engine.EngineInfo;
import org.qubership.integration.platform.engine.model.engine.EngineState;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class EngineStateBuilder {
    @Inject
    EngineInfo engineInfo;

    @Inject
    SourceLoadStateTracker sourceLoadStateTracker;

    public EngineState build(CamelContext camelContext) {
        return EngineState.builder()
                .engine(engineInfo)
                .deployments(buildDeployments(camelContext))
                .build();
    }

    private Map<String, EngineDeployment> buildDeployments(CamelContext camelContext) {
        return sourceLoadStateTracker.getSourceDefinitions().stream()
                .map(source -> buildDeployment(
                        camelContext,
                        source,
                        sourceLoadStateTracker.getLoadState(source.getId())))
                .collect(Collectors.toMap(
                        d -> d.getDeploymentInfo().getDeploymentId(),
                        Function.identity()
                ));
    }

    private EngineDeployment buildDeployment(
            CamelContext camelContext,
            SourceDefinition sourceDefinition,
            SourceLoadStateTracker.SourceLoadState sourceLoadState
    ) {
        return EngineDeployment.builder()
                .deploymentInfo(buildDeploymentInfo(camelContext, sourceDefinition, sourceLoadState))
                .status(buildDeploymentStatus(sourceLoadState))
                .errorMessage(Optional.ofNullable(sourceLoadState.exception())
                        .map(Exception::getMessage)
                        .orElse(null))
                .build();
    }

    private DeploymentInfo buildDeploymentInfo(
            CamelContext camelContext,
            SourceDefinition sourceDefinition,
            SourceLoadStateTracker.SourceLoadState sourceLoadState
    ) {
        // Assuming that source ID is a corresponding chain ID.
        String chainId = sourceDefinition.getId();
        ChainInfo chainInfo = camelContext.getRegistry()
                .findByType(ChainInfo.class)
                .stream()
                // Assuming that source ID is a corresponding chain ID.
                .filter(info -> chainId.equals(info.getId()))
                .findAny()
                .orElse(ChainInfo.builder()
                        .id(chainId)
                        // Assuming that source name is a corresponding chain name.
                        .name(sourceDefinition.getName())
                        .build());
        return DeploymentInfo.builder()
                .deploymentId(chainInfo.getDeploymentId())
                .chainId(chainInfo.getId())
                .chainName(chainInfo.getName())
                .snapshotId(chainInfo.getSnapshotId())
                .snapshotName(chainInfo.getSnapshotName())
                .chainStatusCode(
                        SourceLoadStateTracker.SourceLoadStage.FAILED
                                .equals(sourceLoadState.stage())
                                ? ErrorCode.UNEXPECTED_DEPLOYMENT_ERROR.getCode()
                                : null
                )
                .build();
    }

    private DeploymentStatus buildDeploymentStatus(SourceLoadStateTracker.SourceLoadState sourceLoadState) {
        return switch (sourceLoadState.stage()) {
            case UNKNOWN, PROCESSING -> DeploymentStatus.PROCESSING;
            case FAILED -> DeploymentStatus.FAILED;
            case SUCCESS -> DeploymentStatus.DEPLOYED;
        };
    }
}
