package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.services.GitRepositoryQuarkusApplication;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class GitRepositoryQuarkusApplicationManagedResourceBuilder extends ProdQuarkusApplicationManagedResourceBuilder {

    protected static final String QUARKUS_PLATFORM_VERSION_PROPERTY = "${QUARKUS_PLATFORM_VERSION}";
    protected static final String QUARKUS_PLUGIN_VERSION_PROPERTY = "${QUARKUS-PLUGIN_VERSION}";
    protected static final String QUARKUS_PLATFORM_GROUP_ID_PROPERTY = "${QUARKUS_PLATFORM_GROUP-ID}";

    private final ServiceLoader<GitRepositoryQuarkusApplicationManagedResourceBinding> bindings = ServiceLoader
            .load(GitRepositoryQuarkusApplicationManagedResourceBinding.class);

    private String gitRepository;
    private String gitBranch;
    private String contextDir;
    private String mavenArgs;
    private boolean devMode;

    protected String getGitRepository() {
        return gitRepository;
    }

    protected String getGitBranch() {
        return gitBranch;
    }

    protected String getContextDir() {
        return contextDir;
    }

    protected String getMavenArgs() {
        return mavenArgs;
    }

    protected boolean isDevMode() {
        return devMode;
    }

    protected String getMavenArgsWithVersion() {
        return mavenArgs
                .replaceAll(quote(QUARKUS_PLATFORM_GROUP_ID_PROPERTY), QuarkusProperties.PLATFORM_GROUP_ID.get())
                .replaceAll(quote(QUARKUS_PLUGIN_VERSION_PROPERTY), QuarkusProperties.getPluginVersion())
                .replaceAll(quote(QUARKUS_PLATFORM_VERSION_PROPERTY), QuarkusProperties.getVersion());
    }

    @Override
    public void init(Annotation annotation) {
        GitRepositoryQuarkusApplication metadata = (GitRepositoryQuarkusApplication) annotation;
        gitRepository = metadata.repo();
        gitBranch = metadata.branch();
        contextDir = metadata.contextDir();
        mavenArgs = metadata.mavenArgs();
        devMode = metadata.devMode();
        setArtifactSuffix(metadata.artifact());
        initAppClasses(new Class[0]);
        setPropertiesFile(metadata.properties());
    }

    @Override
    protected QuarkusManagedResource findManagedResource() {
        for (GitRepositoryQuarkusApplicationManagedResourceBinding binding : bindings) {
            if (binding.appliesFor(getContext())) {
                return binding.init(this);
            }
        }

        return new GitRepositoryLocalhostQuarkusApplicationManagedResource(this);
    }

    @Override
    protected Path getTargetFolderForLocalArtifacts() {
        return getApplicationFolder().resolve(TARGET);
    }

    @Override
    protected Path getApplicationFolder() {
        Path appFolder = getContext().getServiceFolder();
        if (StringUtils.isNotEmpty(getContextDir())) {
            appFolder = appFolder.resolve(getContextDir());
        }

        return appFolder;
    }

    @Override
    protected void copyResourcesInFolderToAppFolder(Path folder) {
        // normal apps will create application.properties from parent folder but our source is git project
        super.copyResourcesInFolderToAppFolder(getApplicationFolder().resolve(folder));
    }

    @Override
    protected void copyResourcesToAppFolder() {
        // app folder may not exist when S2I scenario use OpenShift build strategy where we do not clone git project
        if (Files.exists(getApplicationFolder())) {
            super.copyResourcesToAppFolder();
        } else {
            // initialize build time properties from default config sources
            detectBuildTimeProperties(Map.of());
        }
    }
}
