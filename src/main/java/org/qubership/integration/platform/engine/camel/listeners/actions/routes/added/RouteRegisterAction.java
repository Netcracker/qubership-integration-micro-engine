package org.qubership.integration.platform.engine.camel.listeners.actions.routes.added;

import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Route;
import org.apache.camel.spi.CamelEvent;
import org.qubership.integration.platform.engine.camel.listeners.EventProcessingAction;
import org.qubership.integration.platform.engine.camel.listeners.qualifiers.OnRouteAdded;
import org.qubership.integration.platform.engine.metadata.ChainInfo;
import org.qubership.integration.platform.engine.metadata.RouteRegistrationInfo;
import org.qubership.integration.platform.engine.metadata.util.MetadataUtil;
import org.qubership.integration.platform.engine.service.RouteRegistrationService;
import org.qubership.integration.platform.engine.util.InjectUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Slf4j
@OnRouteAdded
@ApplicationScoped
public class RouteRegisterAction implements EventProcessingAction<CamelEvent.RouteAddedEvent> {
    private final Set<String> registeredRouteChainIds = new ConcurrentHashSet<>();
    private final Optional<RouteRegistrationService> routeRegistrationService;

    @Inject
    public RouteRegisterAction(
            Instance<RouteRegistrationService> routeRegistrationService
    ) {
        this.routeRegistrationService = InjectUtil.injectOptional(routeRegistrationService);
    }

    @Override
    public void process(CamelEvent.RouteAddedEvent event) throws Exception {
        Route route = event.getRoute();
        ChainInfo chainInfo = MetadataUtil.getChainInfo(route);
        if (registeredRouteChainIds.add(chainInfo.getId())) {
            routeRegistrationService.ifPresentOrElse(
                    svc -> {
                        Collection<RouteRegistrationInfo> routeRegistrationInfos =
                                MetadataUtil.getRouteRegistrationInfo(route.getCamelContext(), chainInfo.getId());
                        svc.registerRoutes(routeRegistrationInfos);
                    },
                    () -> log.warn("Route registration on Control Plane for chain '{}' ({}) is skipped due to application configuration.",
                            chainInfo.getName(), chainInfo.getId())
            );
        }
    }
}
