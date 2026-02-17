package org.qubership.integration.platform.engine.camel.repository;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;

// TODO Check that it is not used and remove if so.
public class RegistryHelper {
    public static Registry getRegistry(CamelContext camelContext, String deploymentId) {
        return camelContext.getRegistry()
                .findSingleByType(PerDeploymentBeanRepository.class)
                .getRegistry(deploymentId);
    }
}
