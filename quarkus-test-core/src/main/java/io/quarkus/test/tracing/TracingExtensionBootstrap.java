package io.quarkus.test.tracing;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.common.collect.Sets;

import io.quarkus.test.bootstrap.ExtensionBootstrap;
import io.quarkus.test.bootstrap.Service;

public class TracingExtensionBootstrap implements ExtensionBootstrap {

    private static final String JAEGER_HTTP_ENDPOINT_PROPERTY = "ts.jaeger-http-endpoint";

    private QuarkusScenarioTracer quarkusScenarioTracer;

    public TracingExtensionBootstrap() {
        String jaegerHttpEndpoint = System.getProperty(JAEGER_HTTP_ENDPOINT_PROPERTY);
        if (StringUtils.isNotEmpty(jaegerHttpEndpoint)) {
            quarkusScenarioTracer = new QuarkusScenarioTracer(jaegerHttpEndpoint);
        }
    }

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return quarkusScenarioTracer != null;
    }

    @Override
    public void onError(ExtensionContext context, Throwable throwable) {
        quarkusScenarioTracer.finishWithError(context, throwable);
    }

    @Override
    public void onServiceStarted(ExtensionContext context, Service service) {
        quarkusScenarioTracer.finishWithSuccess(context, Sets.newHashSet(service.getName()));
    }

    @Override
    public void onServiceInitiate(ExtensionContext context, Service service) {
        quarkusScenarioTracer.createSpanContext(context);
    }

    @Override
    public void onSuccess(ExtensionContext context) {
        quarkusScenarioTracer.finishWithSuccess(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        quarkusScenarioTracer.createSpanContext(context);
    }
}
