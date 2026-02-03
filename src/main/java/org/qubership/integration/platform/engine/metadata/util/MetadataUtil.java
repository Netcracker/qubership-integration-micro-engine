package org.qubership.integration.platform.engine.metadata.util;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.spi.ClassResolver;
import org.qubership.integration.platform.engine.metadata.*;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.qubership.integration.platform.engine.model.ChainElementType.CHECKPOINT;

public class MetadataUtil {
    private MetadataUtil() {
    }

    private static String getBeanName(Class<?> cls, String id) {
        return String.format("%s-%s", cls.getSimpleName(), id);
    }

    private static <T> T getBeanForChain(Exchange exchange, Class<T> cls) {
        String chainId = getChainId(exchange);
        return exchange.getContext().getRegistry().lookupByNameAndType(getBeanName(cls, chainId), cls);
    }

    private static <T> T getBeanForChain(Route route, Class<T> cls) {
        String chainId = getChainId(route);
        return route.getCamelContext().getRegistry().lookupByNameAndType(getBeanName(cls, chainId), cls);
    }

    private static <T> T getBeanForElement(Exchange exchange, String elementId, Class<T> cls) {
        return exchange.getContext().getRegistry().lookupByNameAndType(getBeanName(cls, elementId), cls);
    }

    private static <T> T getBeanForElement(Route route, String elementId, Class<T> cls) {
        return route.getCamelContext().getRegistry().lookupByNameAndType(getBeanName(cls, elementId), cls);
    }

    private static <T> Optional<T> getOptionalBeanForElement(Exchange exchange, String elementId, Class<T> cls) {
        return Optional.ofNullable(getBeanForElement(exchange, elementId, cls));
    }

    private static <T> Optional<T> getOptionalBeanForElement(Route route, String elementId, Class<T> cls) {
        return Optional.ofNullable(getBeanForElement(route, elementId, cls));
    }

    public static <T> boolean hasBeanForChain(Route route, Class<T> cls) {
        String chainId = getChainId(route);
        return nonNull(getBeanForChain(route, cls));
    }

    public static <T> void addBeanForChain(Route route, Class<T> cls, T obj) {
        String chainId = getChainId(route);
        String beanName = getBeanName(cls, chainId);
        route.getCamelContext().getRegistry().bind(beanName, obj);
    }

    public static String getChainId(Exchange exchange) {
        String routeId = exchange.getFromRouteId();
        Route route = exchange.getContext().getRoute(routeId);
        return getChainId(route);
    }

    public static String getChainId(Route route) {
        return route.getGroup();
    }

    public static ChainInfo getChainInfo(Exchange exchange) {
        return getBeanForChain(exchange, ChainInfo.class);
    }

    public static ChainInfo getChainInfo(Route route) {
        return getBeanForChain(route, ChainInfo.class);
    }

    public static Stream<ElementInfo> getChainElementsInfo(Exchange exchange) {
        ChainInfo chainInfo = getChainInfo(exchange);
        return exchange.getContext().getRegistry().findByType(ElementInfo.class)
                .stream()
                .filter(elementInfo -> chainInfo.getId().equals(elementInfo.getChainId()));
    }

    public static Stream<ElementInfo> getChainElementsInfo(Route route) {
        ChainInfo chainInfo = getChainInfo(route);
        return route.getCamelContext().getRegistry().findByType(ElementInfo.class)
                .stream()
                .filter(elementInfo -> chainInfo.getId().equals(elementInfo.getChainId()));
    }

    public static boolean chainHasCheckpointElements(Exchange exchange) {
        return getChainElementsInfo(exchange)
                .anyMatch(elementInfo -> CHECKPOINT.getText().equals(elementInfo.getType()));
    }

    public static MaskedFields getMaskedFields(Exchange exchange) {
        return getBeanForChain(exchange, MaskedFields.class);
    }

    public static Optional<ElementInfo> getElementInfo(Exchange exchange, String elementId) {
        return getOptionalBeanForElement(exchange, elementId, ElementInfo.class);
    }

    public static Optional<WireTapInfo> getWireTapInfo(Exchange exchange, String elementId) {
        return getOptionalBeanForElement(exchange, elementId, WireTapInfo.class);
    }

    public static Optional<ServiceCallInfo> getServiceCallInfo(Exchange exchange, String elementId) {
        return getOptionalBeanForElement(exchange, elementId, ServiceCallInfo.class);
    }

    public static Optional<ServiceCallInfo> getServiceCallInfo(Route route, String elementId) {
        return getOptionalBeanForElement(route, elementId, ServiceCallInfo.class);
    }

    public static Collection<RouteRegistrationInfo> getRouteRegistrationInfo(CamelContext context, String chainId) {
        return context.getRegistry()
                .findByType(RouteRegistrationInfo.class).stream()
                .filter(routeRegistrationInfo -> chainId.equals(routeRegistrationInfo.getChainId()))
                .collect(Collectors.toList());
    }

    // FIXME
    public static ClassResolver getClassResolver(Exchange exchange) {
        return getBeanForChain(exchange, ClassResolver.class);
    }
}
