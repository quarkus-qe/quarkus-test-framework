package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.cloneRepository;
import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.getEffectivePropertiesForGitRepository;
import static io.quarkus.test.services.quarkus.GitRepositoryResourceBuilderUtils.mavenBuild;

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
        List<String> effectiveProperties = getEffectivePropertiesForGitRepository(systemProperties);

        // Dev mode
        if (model.isDevMode()) {
            return MavenUtils.devModeMavenCommand(model.getContext(), effectiveProperties);
        }

        // JVM or Native
        return super.prepareCommand(effectiveProperties);
    }

    @Override
    protected LaunchMode getLaunchMode() {
        if (model.isDevMode()) {
            return LaunchMode.DEV;
        }

        return super.getLaunchMode();
    }

}
