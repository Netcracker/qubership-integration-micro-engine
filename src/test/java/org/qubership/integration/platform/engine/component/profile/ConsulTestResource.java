package org.qubership.integration.platform.engine.component.profile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Map;

public class ConsulTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName CONSUL_IMAGE = DockerImageName.parse("hashicorp/consul:1.15.4");
    private static final int CONSUL_HTTP_PORT = 8500;
    private static final String MANAGEMENT_TOKEN = "556339d5-f6a3-ff3b-ce1d-98e5b70d4a13";

    private GenericContainer<?> consul;

    @Override
    public Map<String, String> start() {
        consul = new GenericContainer<>(CONSUL_IMAGE)
                .withExposedPorts(CONSUL_HTTP_PORT)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("consul/server.json"),
                        "/consul/config/server.json"
                )
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("consul/consul-acl.json"),
                        "/consul/config/consul-acl.json"
                )
                .withCommand("agent", "-bootstrap-expect=1")
                .waitingFor(
                        Wait.forHttp("/v1/status/leader")
                                .forStatusCode(200)
                )
                .withStartupTimeout(Duration.ofSeconds(90));

        consul.start();

        String consulUrl = "http://" + consul.getHost() + ":" + consul.getMappedPort(CONSUL_HTTP_PORT);

        return Map.ofEntries(
                Map.entry("CONSUL_URL", consulUrl),
                Map.entry("consul.url", consulUrl),
                Map.entry("quarkus.consul-source-config.agent.url", consulUrl),

                Map.entry("consul.token", MANAGEMENT_TOKEN),
                Map.entry("CONSUL_HTTP_TOKEN", MANAGEMENT_TOKEN)
        );
    }

    @Override
    public void stop() {
        if (consul != null) {
            consul.stop();
            consul = null;
        }
    }
}
