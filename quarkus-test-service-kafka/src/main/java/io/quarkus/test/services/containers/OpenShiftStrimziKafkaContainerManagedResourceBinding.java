package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.scenarios.OpenShiftScenario;

public class OpenShiftStrimziKafkaContainerManagedResourceBinding implements KafkaContainerManagedResourceBinding {

    @Override
    public boolean appliesFor(KafkaContainerManagedResourceBuilder builder) {
        return builder.getVendor().isStrimzi()
                && builder.getContext().getTestContext().getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public ManagedResource init(KafkaContainerManagedResourceBuilder builder) {
        return new OpenShiftStrimziKafkaContainerManagedResource(builder);
    }

}
