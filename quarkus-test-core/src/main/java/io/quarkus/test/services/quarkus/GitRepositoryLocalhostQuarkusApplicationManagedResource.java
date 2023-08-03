package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.cloneRepository;
import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.mavenBuild;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_GROUP_ID;
import static io.quarkus.test.utils.MavenUtils.withProperty;

import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.utils.MavenUtils;

public class GitRepositoryLocalhostQuarkusApplicationManagedResource
        extends ProdLocalhostQuarkusApplicationManagedResource {

    private final GitRepositoryQuarkusApplicationManagedResourceBuilder model;

    public GitRepositoryLocalhostQuarkusApplicationManagedResource(
            GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void onPreBuild() {
        super.onPreBuild();

        cloneRepository(model);
        mavenBuild(model);
    }

    @Override
    protected Path getApplicationFolder() {
        Path appFolder = model.getContext().getServiceFolder();
        if (StringUtils.isNotEmpty(model.getContextDir())) {
            appFolder = appFolder.resolve(model.getContextDir());
        }

        return appFolder;
    }

    @Override
    protected List<String> prepareCommand(List<String> systemProperties) {
        final List<String> commands;

        if (model.isDevMode()) {
            // Dev mode
            commands = MavenUtils.devModeMavenCommand(model.getContext(), systemProperties);
        } else {
            // JVM or Native
            commands = super.prepareCommand(systemProperties);
        }

        // set quarkus.platform.group-id
        commands.add(withProperty(PLATFORM_GROUP_ID.getPropertyKey(), PLATFORM_GROUP_ID.get()));

        return List.copyOf(commands);
    }

    @Override
    protected LaunchMode getLaunchMode() {
        if (model.isDevMode()) {
            return LaunchMode.DEV;
        }

        return super.getLaunchMode();
    }

}
