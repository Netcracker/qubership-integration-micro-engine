package org.qubership.integration.platform.engine.camel.listeners.actions.routes.added;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Route;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.ClassResolver;
import org.qubership.integration.platform.engine.camel.QipCustomClassResolver;
import org.qubership.integration.platform.engine.camel.listeners.EventProcessingAction;
import org.qubership.integration.platform.engine.camel.listeners.qualifiers.OnRouteAdded;
import org.qubership.integration.platform.engine.metadata.ServiceCallInfo;
import org.qubership.integration.platform.engine.metadata.util.MetadataUtil;
import org.qubership.integration.platform.engine.service.ExternalLibraryService;

import java.util.Collection;
import java.util.Optional;

import static org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties.SERVICE_CALL_ELEMENT;

@Slf4j
@OnRouteAdded
@ApplicationScoped
public class AddClassResolverAction implements EventProcessingAction<CamelEvent.RouteAddedEvent> {
    @Inject
    ExternalLibraryService externalLibraryService;

    @Override
    public void process(CamelEvent.RouteAddedEvent event) throws Exception {
        Route route = event.getRoute();
        if (MetadataUtil.hasBeanForChain(route, ClassResolver.class)) {
            return;
        }
        String chainId = MetadataUtil.getChainId(route);
        log.debug("Adding class resolver for chain {} to the Camel context", chainId);

        Collection<String> specificationIds = getSpecificationIds(route);
        ClassLoader classLoader = externalLibraryService.getClassLoaderForSpecifications(
                specificationIds, route.getCamelContext().getApplicationContextClassLoader());
        ClassResolver classResolver = new QipCustomClassResolver(classLoader);
        MetadataUtil.addBeanForChain(route, ClassResolver.class, classResolver);
    }

    private Collection<String> getSpecificationIds(Route route) {
        return MetadataUtil.getChainElementsInfo(route)
                .filter(info -> SERVICE_CALL_ELEMENT.equals(info.getType()))
                .map(info -> MetadataUtil.getServiceCallInfo(route, info.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ServiceCallInfo::getSpecificationId)
                .toList();
    }
}
