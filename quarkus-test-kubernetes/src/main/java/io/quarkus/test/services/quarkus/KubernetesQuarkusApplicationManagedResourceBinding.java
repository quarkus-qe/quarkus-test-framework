package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.KubernetesScenario;

public class KubernetesQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public QuarkusManagedResource init(QuarkusApplicationManagedResourceBuilder builder) {
        return new KubernetesQuarkusApplicationManagedResource(builder);
    }

}
