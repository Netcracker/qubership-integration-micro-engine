package org.qubership.integration.platform.engine.camel.dsl.errorhandling;

//import org.apache.camel.CamelContext;
//import org.apache.camel.builder.RouteBuilder;
//import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
//
//import java.util.Collections;
//import java.util.Set;

public class RouteBuilderWrapper /*extends RouteBuilder*/ {
    //    private final RouteBuilder delegate;
    //    private final ErrorHandler errorHandler;
    //
    //    public RouteBuilderWrapper(RouteBuilder delegate, ErrorHandler errorHandler) {
    //        super();
    //        this.delegate = delegate;
    //        this.errorHandler = errorHandler;
    //    }
    //
    //    @Override
    //    public void configure() throws Exception {
    //        delegate.configure();
    //    }
    //
    //    @Override
    //    public void addRoutesToCamelContext(CamelContext context) throws Exception {
    //        try {
    //            delegate.addRoutesToCamelContext(context);
    //        } catch (Exception e) {
    //            errorHandler.handleError(context, e);
    //        }
    //    }
    //
    //    @Override
    //    public void addTemplatedRoutesToCamelContext(CamelContext context) throws Exception {
    //        try {
    //            delegate.addTemplatedRoutesToCamelContext(context);
    //        } catch (Exception e) {
    //            errorHandler.handleError(context, e);
    //        }
    //    }
    //
    //    @Override
    //    public Set<String> updateRoutesToCamelContext(CamelContext context) throws Exception {
    //        try {
    //            return delegate.updateRoutesToCamelContext(context);
    //        } catch (Exception e) {
    //            errorHandler.handleError(context, e);
    //            return Collections.emptySet();
    //        }
    //    }
    //
    //    @Override
    //    public void addLifecycleInterceptor(RouteBuilderLifecycleStrategy interceptor) {
    //        delegate.addLifecycleInterceptor(interceptor);
    //    }
}
