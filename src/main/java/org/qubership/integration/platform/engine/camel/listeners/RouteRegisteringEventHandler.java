package org.qubership.integration.platform.engine.camel.listeners;

import io.quarkus.arc.Unremovable;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Route;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.SimpleEventNotifierSupport;
import org.qubership.integration.platform.engine.metadata.ChainInfo;
import org.qubership.integration.platform.engine.metadata.RouteRegistrationInfo;
import org.qubership.integration.platform.engine.metadata.util.MetadataUtil;
import org.qubership.integration.platform.engine.service.RouteRegistrationService;
import org.qubership.integration.platform.engine.util.InjectUtil;

import java.util.*;

@Slf4j
@ApplicationScoped
@Unremovable
public class RouteRegisteringEventHandler extends SimpleEventNotifierSupport {
    private final Set<String> registeredRouteChainIds = new ConcurrentHashSet<>();
    private final Optional<RouteRegistrationService> routeRegistrationService;

    @Inject
    public RouteRegisteringEventHandler(
            Instance<RouteRegistrationService> routeRegistrationService
    ) {
        this.routeRegistrationService = InjectUtil.injectOptional(routeRegistrationService);
    }

    @Override
    public void notify(CamelEvent event) throws Exception {
        if (event instanceof CamelEvent.RouteAddedEvent routeAddedEvent) {
            Route route = routeAddedEvent.getRoute();
            ChainInfo chainInfo = MetadataUtil.getChainInfo(route);
            if (registeredRouteChainIds.add(chainInfo.getId())) {
                routeRegistrationService.ifPresentOrElse(
                        svc -> {
                            Collection<RouteRegistrationInfo> routeRegistrationInfos =
                                    MetadataUtil.getRouteRegistrationInfo(route.getCamelContext(), chainInfo.getId());
                            svc.registerRoutes(routeRegistrationInfos);
                        },
                        () -> log.warn("Route registration on Control Plane for chain '{}' ({}) skipped due to application configuration",
                                chainInfo.getName(), chainInfo.getId())
                );
            }

        }
    }
}
