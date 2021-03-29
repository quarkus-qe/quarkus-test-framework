package io.quarkus.test.services.quarkus;

import java.util.Arrays;
import java.util.List;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class ExtensionOpenShiftQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {

    private static final List<OpenShiftDeploymentStrategy> SUPPORTED = Arrays.asList(
            OpenShiftDeploymentStrategy.UsingOpenShiftExtension,
            OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy);

    @Override
    public boolean appliesFor(ServiceContext context) {
        OpenShiftScenario annotation = context.getTestContext().getRequiredTestClass().getAnnotation(OpenShiftScenario.class);
        return annotation != null && SUPPORTED.contains(annotation.deployment());
    }

    @Override
    public QuarkusManagedResource init(QuarkusApplicationManagedResourceBuilder builder) {
        return new ExtensionOpenShiftQuarkusApplicationManagedResource(builder);
    }

}
