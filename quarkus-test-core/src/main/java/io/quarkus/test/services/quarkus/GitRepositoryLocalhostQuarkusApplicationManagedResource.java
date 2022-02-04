package io.quarkus.test.services.quarkus;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.utils.GitUtils;
import io.quarkus.test.utils.MavenUtils;

public class GitRepositoryLocalhostQuarkusApplicationManagedResource
        extends ProdLocalhostQuarkusApplicationManagedResource {

    private static final String QUARKUS_VERSION = "quarkus.version";
    private static final String QUARKUS_PLUGIN_VERSION = "quarkus.plugin.version";
    private static final String QUARKUS_VERSION_VALUE = "${quarkus.platform.version}";
    private static final String QUARKUS_PLUGIN_VERSION_VALUE = "${quarkus-plugin.version}";

    private final GitRepositoryQuarkusApplicationManagedResourceBuilder model;

    public GitRepositoryLocalhostQuarkusApplicationManagedResource(
            GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void onPreBuild() {
        super.onPreBuild();

        // Clone repository
        GitUtils.cloneRepository(model.getContext(), model.getGitRepository());
        if (StringUtils.isNotEmpty(model.getGitBranch())) {
            GitUtils.checkoutBranch(model.getContext(), model.getGitBranch());
        }

        // Maven build
        String[] mvnArgs = StringUtils.split(model.getMavenArgsWithVersion(), " ");
        List<String> effectiveProperties = getEffectivePropertiesForGitRepository(Arrays.asList(mvnArgs));
        MavenUtils.build(model.getContext(), getApplicationFolder(), effectiveProperties);
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

    private List<String> getEffectivePropertiesForGitRepository(List<String> properties) {
        List<String> effectiveProperties = new LinkedList<>(properties);
        // Override quarkus plugin version.
        effectiveProperties.add(MavenUtils.withProperty(QUARKUS_VERSION, QUARKUS_VERSION_VALUE));
        effectiveProperties.add(MavenUtils.withProperty(QUARKUS_PLUGIN_VERSION, QUARKUS_PLUGIN_VERSION_VALUE));

        return effectiveProperties;
    }
}
