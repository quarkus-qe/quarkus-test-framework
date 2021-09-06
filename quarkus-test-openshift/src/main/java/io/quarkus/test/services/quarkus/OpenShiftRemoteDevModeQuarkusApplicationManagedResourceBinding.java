package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftRemoteDevModeQuarkusApplicationManagedResourceBinding
        implements RemoteDevModeQuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public QuarkusManagedResource init(RemoteDevModeQuarkusApplicationManagedResourceBuilder builder) {
        return new RemoteDevModeBuildOpenShiftQuarkusApplicationManagedResource(builder);
    }
}
