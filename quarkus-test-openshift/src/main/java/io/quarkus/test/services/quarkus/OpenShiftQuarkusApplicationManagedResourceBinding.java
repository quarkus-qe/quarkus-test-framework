package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {

    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public QuarkusManagedResource init(QuarkusApplicationManagedResourceBuilder builder) {
        OpenShiftScenario annotation = builder.getContext().getTestContext().getRequiredTestClass()
                .getAnnotation(OpenShiftScenario.class);

        try {
            return annotation.deployment().getStrategyClass()
                    .getDeclaredConstructor(QuarkusApplicationManagedResourceBuilder.class)
                    .newInstance(builder);
        } catch (Exception exception) {
            fail("Failed to load OpenShift strategy. Caused by " + exception.getMessage());
        }

        return null;
    }

}
