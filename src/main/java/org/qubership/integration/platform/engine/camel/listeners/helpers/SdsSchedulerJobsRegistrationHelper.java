package org.qubership.integration.platform.engine.camel.listeners.helpers;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.qubership.integration.platform.engine.metadata.ChainInfo;
import org.qubership.integration.platform.engine.metadata.ElementInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@IfBuildProperty(name = "qip.sds.enabled", stringValue = "true")
public class SdsSchedulerJobsRegistrationHelper {
    // chain ID -> [element ID]
    private final Map<String, Set<String>> registeredJobs = new ConcurrentHashMap<>();

    public boolean registered(ChainInfo chainInfo) {
        return registeredJobs.containsKey(chainInfo.getId());
    }

    public boolean registered(ChainInfo chainInfo, ElementInfo elementInfo) {
        return registeredJobs.getOrDefault(chainInfo.getId(), Collections.emptySet()).contains(elementInfo.getId());
    }

    public void markRegistered(ChainInfo chainInfo, ElementInfo elementInfo) {
        registeredJobs.compute(chainInfo.getId(), (key, value) -> {
            if (value == null) {
                return Collections.singleton(elementInfo.getId());
            } else {
                Set<String> result = new HashSet<>(value);
                result.add(elementInfo.getId());
                return result;
            }
        });
    }

    public void markUnregistered(ChainInfo chainInfo) {
        registeredJobs.remove(chainInfo.getId());
    }
}
