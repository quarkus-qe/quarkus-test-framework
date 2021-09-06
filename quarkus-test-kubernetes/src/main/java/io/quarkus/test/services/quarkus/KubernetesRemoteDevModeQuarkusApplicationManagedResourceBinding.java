package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.KubernetesScenario;

public class KubernetesRemoteDevModeQuarkusApplicationManagedResourceBinding
        implements RemoteDevModeQuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public QuarkusManagedResource init(RemoteDevModeQuarkusApplicationManagedResourceBuilder builder) {
        return new RemoteDevModeKubernetesQuarkusApplicationManagedResource(builder);
    }
}
