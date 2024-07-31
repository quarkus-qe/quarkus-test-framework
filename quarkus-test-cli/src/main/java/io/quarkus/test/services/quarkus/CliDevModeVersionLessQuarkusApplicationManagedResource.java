package io.quarkus.test.services.quarkus;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.ServiceContext;

public class CliDevModeVersionLessQuarkusApplicationManagedResource
        extends CliDevModeLocalhostQuarkusApplicationManagedResource {
    public CliDevModeVersionLessQuarkusApplicationManagedResource(ServiceContext serviceContext, QuarkusCliClient client) {
        super(serviceContext, client);
    }

    @Override
    protected Map<String, String> getPropertiesForCommand() {
        Map<String, String> runtimeProperties = new HashMap<>(serviceContext.getOwner().getProperties());
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, "" + assignedHttpPort);
        return runtimeProperties;
    }
}
