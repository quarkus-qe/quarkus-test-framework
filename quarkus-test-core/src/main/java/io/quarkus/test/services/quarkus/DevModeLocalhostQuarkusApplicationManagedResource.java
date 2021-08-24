package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.MavenUtils.devModeMavenCommand;
import static io.quarkus.test.utils.MavenUtils.installParentPomsIfNeeded;

import java.util.List;

import io.quarkus.test.services.quarkus.model.LaunchMode;

public class DevModeLocalhostQuarkusApplicationManagedResource extends LocalhostQuarkusApplicationManagedResource {

    private final DevModeQuarkusApplicationManagedResourceBuilder model;

    public DevModeLocalhostQuarkusApplicationManagedResource(DevModeQuarkusApplicationManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    @Override
    protected LaunchMode getLaunchMode() {
        return LaunchMode.DEV;
    }

    protected List<String> prepareCommand(List<String> systemProperties) {
        installParentPomsIfNeeded();

        return devModeMavenCommand(model.getContext(), systemProperties);
    }

}
