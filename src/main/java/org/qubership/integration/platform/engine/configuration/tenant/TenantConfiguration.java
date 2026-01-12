package org.qubership.integration.platform.engine.configuration.tenant;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantConfiguration {
    @Getter
    @ConfigProperty(name = "tenant.default.id")
    String defaultTenant;

    @PostConstruct
    public void init() {
        // TODO configure tenant provider
        //        if (StringUtils.isEmpty(defaultTenant)) {
        //            throw new BeanInitializationException(
        //                    "Default tenant is empty! Please specify application property [tenant.default.id]");
        //        }
        //
        //        log.info("Default tenant id [tenant.default.id]: {}", defaultTenant);
        //        this.defaultTenant = defaultTenant;
        //        ContextManager.register(Collections.singletonList(new AppDefaultTenantProvider(defaultTenant)));
    }
}
