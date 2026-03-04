package org.qubership.integration.platform.engine.kafka;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.qubership.integration.platform.engine.model.opensearch.KafkaQueueElement;

@ApplicationScoped
@IfBuildProfile("dbaas")
@LookupIfProperty(name = "qip.opensearch.kafka-client.enabled", stringValue = "true")
public class MaasOpensearchKafkaProducer implements OpenSearchKafkaProducer {
    @ConfigProperty(name = "qip.opensearch.kafka-client.maas-producer-name")
    String producerName;

    @Inject
    KafkaMaaSClient kafkaClient;

    @Override
    public void send(String key, KafkaQueueElement kafkaQueueElement) {
        // TODO
    }
}
