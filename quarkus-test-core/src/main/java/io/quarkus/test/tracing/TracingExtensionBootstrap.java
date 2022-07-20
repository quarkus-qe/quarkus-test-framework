package io.quarkus.test.tracing;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.ExtensionBootstrap;
import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.configuration.PropertyLookup;

public class TracingExtensionBootstrap implements ExtensionBootstrap {

    public static final String TRACING_ID = "tracing";

    private static final PropertyLookup JAEGER_HTTP_ENDPOINT_SYSTEM_PROPERTY = new PropertyLookup("tracing.jaeger.endpoint");

    private QuarkusScenarioTracer quarkusScenarioTracer;

    public TracingExtensionBootstrap() {
        String jaegerHttpEndpoint = JAEGER_HTTP_ENDPOINT_SYSTEM_PROPERTY.get();
        if (StringUtils.isNotEmpty(jaegerHttpEndpoint)) {
            quarkusScenarioTracer = new QuarkusScenarioTracer(jaegerHttpEndpoint);
        }
    }

    @Override
    public boolean appliesFor(ScenarioContext context) {
        return quarkusScenarioTracer != null;
    }

    @Override
    public void beforeAll(ScenarioContext context) {
        context.getTestStore().put(TRACING_ID, quarkusScenarioTracer);
        // Include span per test class only
        quarkusScenarioTracer.createSpanContext(context);
    }

    @Override
    public void afterAll(ScenarioContext context) {
        quarkusScenarioTracer.onShutdown();
    }

    @Override
    public void beforeEach(ScenarioContext context) {
        // Include span per test class ++ test method
        quarkusScenarioTracer.createSpanContext(context);
    }

    @Override
    public void onError(ScenarioContext context, Throwable throwable) {
        quarkusScenarioTracer.finishWithError(context, throwable);
    }

    @Override
    public void onSuccess(ScenarioContext context) {
        quarkusScenarioTracer.finishWithSuccess(context);
    }
}
