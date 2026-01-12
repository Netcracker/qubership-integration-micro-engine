package org.qubership.integration.platform.engine.consul;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@IfBuildProperty(name = "m2m.enabled", stringValue = "false")
public class ConsulTokenStorage implements TokenStorage {
    @ConfigProperty(name = "consul.token")
    String token;

    @Override
    public String get() {
        return token;
    }

    @Override
    public void update(String token) {
        // Do nothing
    }
}
