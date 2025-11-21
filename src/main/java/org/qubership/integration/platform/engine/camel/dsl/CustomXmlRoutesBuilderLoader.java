package org.qubership.integration.platform.engine.camel.dsl;

import jakarta.enterprise.inject.spi.CDI;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.annotations.RoutesLoader;
import org.qubership.integration.platform.engine.camel.dsl.errorhandling.ErrorHandler;
import org.qubership.integration.platform.engine.camel.dsl.errorhandling.ErrorHandlerFactory;
//import org.qubership.integration.platform.engine.camel.dsl.errorhandling.RouteBuilderWrapper;
import org.qubership.integration.platform.engine.camel.dsl.preprocess.ResourceContentPreprocessingService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import static java.util.Objects.isNull;

// Can't make this class an application scoped bean because
// in that case Camel will fail to inject context in it.
// So it has to be managed by Camel itself via resource
// META-INF/services/org/apache/camel/routes-loader/xml.
// The downfall is that CDI has to be used explicitly to
// inject dependencies.

@Slf4j
@ManagedResource(description = "Managed XML RoutesBuilderLoader")
@RoutesLoader(XmlRoutesBuilderLoader.EXTENSION)
public class CustomXmlRoutesBuilderLoader extends XmlRoutesBuilderLoader {
    private ResourceContentPreprocessingService preprocessingService;
    private ErrorHandlerFactory errorHandlerFactory;

    @Override
    public void preParseRoute(Resource resource) throws Exception {
        super.preParseRoute(resource);
    }

    @Override
    public RouteBuilder doLoadRouteBuilder(Resource input) throws Exception {
        Resource preprocessedInput = preprocessInput(input);
        return wrapRouteBuilder(input, super.doLoadRouteBuilder(preprocessedInput));
    }

    private Resource preprocessInput(Resource input) throws Exception {
        ResourceContentPreprocessingService preprocessingService = getPreprocessingService();
        String content = new String(input.getInputStream().readAllBytes());
        content = preprocessingService.preprocess(content);
        return new SimpleResource(
                input.getScheme(),
                input.getLocation(),
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private synchronized ResourceContentPreprocessingService getPreprocessingService() {
        if (isNull(preprocessingService)) {
            preprocessingService = CDI.current()
                    .select(ResourceContentPreprocessingService.class).get();
        }
        return preprocessingService;
    }

    private RouteBuilder wrapRouteBuilder(Resource resource, RouteBuilder builder) {
        ErrorHandler errorHandler = getErrorHandler(resource);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                builder.configure();
            }

            @Override
            public void addRoutesToCamelContext(CamelContext context) throws Exception {
                try {
                    builder.addRoutesToCamelContext(context);
                } catch (Exception e) {
                    errorHandler.handleError(context, e);
                }
            }

            @Override
            public void addTemplatedRoutesToCamelContext(CamelContext context) throws Exception {
                try {
                    builder.addTemplatedRoutesToCamelContext(context);
                } catch (Exception e) {
                    errorHandler.handleError(context, e);
                }
            }

            @Override
            public Set<String> updateRoutesToCamelContext(CamelContext context) throws Exception {
                try {
                    return builder.updateRoutesToCamelContext(context);
                } catch (Exception e) {
                    errorHandler.handleError(context, e);
                    return Collections.emptySet();
                }
            }

            @Override
            public void addLifecycleInterceptor(RouteBuilderLifecycleStrategy interceptor) {
                builder.addLifecycleInterceptor(interceptor);
            }
        }; // new RouteBuilderWrapper(builder, errorHandler);
    }

    private ErrorHandler getErrorHandler(Resource resource) {
        return getErrorHandlerFactory().getErrorHandler(resource);
    }

    private synchronized ErrorHandlerFactory getErrorHandlerFactory() {
        if (isNull(errorHandlerFactory)) {
            errorHandlerFactory = CDI.current().select(ErrorHandlerFactory.class).get();
        }
        return errorHandlerFactory;
    }
}
