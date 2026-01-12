package org.qubership.integration.platform.engine.consul;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// TODO M2M
@ApplicationScoped
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
