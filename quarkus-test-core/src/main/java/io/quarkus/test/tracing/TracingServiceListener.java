package io.quarkus.test.tracing;

import java.util.Optional;

import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.ServiceListener;

public class TracingServiceListener implements ServiceListener {

    @Override
    public void onServiceStarted(ServiceContext service) {
        getTracer(service.getScenarioContext())
                .ifPresent(tracer -> tracer.updateWithAttribute(service.getScenarioContext(), service.getName()));
    }

    @Override
    public void onServiceError(ServiceContext service, Throwable throwable) {
        getTracer(service.getScenarioContext())
                .ifPresent(tracer -> tracer.finishWithError(service.getScenarioContext(), throwable, service.getName()));
    }

    @Override
    public void onServiceStopped(ServiceContext service) {
        getTracer(service.getScenarioContext())
                .ifPresent(tracer -> tracer.finishWithSuccess(toClassScenarioContext(service)));
    }

    private ScenarioContext toClassScenarioContext(ServiceContext service) {
        // remove method test context as we want to end the class span
        return service.getScenarioContext().toClassScenarioContext();
    }

    private Optional<QuarkusScenarioTracer> getTracer(ScenarioContext scenario) {
        Object tracer = scenario.getTestStore().get(TracingExtensionBootstrap.TRACING_ID);
        if (tracer == null) {
            // Tracing is off
            return Optional.empty();
        }

        return Optional.of((QuarkusScenarioTracer) tracer);
    }
}
