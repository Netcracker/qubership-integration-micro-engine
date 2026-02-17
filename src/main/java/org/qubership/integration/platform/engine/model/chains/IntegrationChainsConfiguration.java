package org.qubership.integration.platform.engine.model.chains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.camel.k.SourceDefinition;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationChainsConfiguration {
    private List<SourceDefinition> sources = Collections.emptyList();
    private List<LibraryDefinition> libraries = Collections.emptyList();
}
