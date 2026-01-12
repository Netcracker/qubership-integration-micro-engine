package org.qubership.integration.platform.engine.consul;

import com.netcracker.cloud.quarkus.security.auth.M2MManager;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.consul.ConsulClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;

@ApplicationScoped
public class ConsulRestClientProducer {
    @ConfigProperty(name = "consul.url")
    URI uri;

    @ConfigProperty(name = "consul.token")
    String token;

    @ConfigProperty(name = "m2m.enabled")
    boolean m2mEnabled;

    @Produces
    public ConsulClient consulClient(Vertx vertx) {
        ConsulClientOptions options = new ConsulClientOptions(uri)
                .setAclToken(getToken());
        return ConsulClient.create(vertx, options);
    }

    private String getToken() {
        return m2mEnabled ? M2MManager.getInstance().getToken().getTokenValue() : token;
    }
}
