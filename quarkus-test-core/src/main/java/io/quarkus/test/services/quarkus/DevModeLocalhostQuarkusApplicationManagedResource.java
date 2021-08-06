package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.MavenUtils.SKIP_CHECKSTYLE;
import static io.quarkus.test.utils.MavenUtils.SKIP_ITS;
import static io.quarkus.test.utils.MavenUtils.installParentPomsIfNeeded;
import static io.quarkus.test.utils.MavenUtils.mvnCommand;
import static io.quarkus.test.utils.MavenUtils.withProperty;

import java.util.Arrays;
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

        List<String> command = mvnCommand(model.getContext());
        command.addAll(Arrays.asList(SKIP_CHECKSTYLE, SKIP_ITS));
        command.addAll(systemProperties);
        command.add(withProperty("debug", "false"));
        command.add("quarkus:dev");

        return command;
    }

}
