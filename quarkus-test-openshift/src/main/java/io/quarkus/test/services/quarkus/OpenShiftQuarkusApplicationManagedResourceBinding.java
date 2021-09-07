package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public QuarkusManagedResource init(ProdQuarkusApplicationManagedResourceBuilder builder) {
        OpenShiftScenario annotation = builder.getContext().getTestContext().getRequiredTestClass()
                .getAnnotation(OpenShiftScenario.class);

        return annotation.deployment().getManagedResource(builder);
    }

}
