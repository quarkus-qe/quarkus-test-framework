package io.quarkus.test.tracing;

import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.common.collect.Sets;

import io.quarkus.test.bootstrap.ExtensionBootstrap;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.scenarios.QuarkusScenario;

public class TracingExtensionBootstrap implements ExtensionBootstrap {

    private final QuarkusScenarioTracer quarkusScenarioTracer = new QuarkusScenarioTracer();

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(QuarkusScenario.class);
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
