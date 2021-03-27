package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.KubernetesScenario;

public class KubernetesQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public ManagedResource init(QuarkusApplicationManagedResourceBuilder builder) {
        return new KubernetesQuarkusApplicationManagedResource(builder);
    }

}
