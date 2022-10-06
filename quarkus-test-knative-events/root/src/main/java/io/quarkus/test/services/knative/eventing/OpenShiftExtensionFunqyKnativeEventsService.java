package io.quarkus.test.services.knative.eventing;

import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.bootstrap.ServiceContext;

public class OpenShiftExtensionFunqyKnativeEventsService extends FunqyKnativeEventsService {

    @Override
    public ServiceContext register(String serviceName, ScenarioContext context) {
        // we set deployment target and registry so that every test don't have to do it (and it's in one place)
        final String serviceScopePrefix = "ts." + serviceName;
        System.setProperty(serviceScopePrefix + ".quarkus.kubernetes.deployment-target", "knative");
        System.setProperty(serviceScopePrefix + ".quarkus.container-image.registry",
                "image-registry.openshift-image-registry.svc:5000");
        return super.register(serviceName, context);
    }

}
