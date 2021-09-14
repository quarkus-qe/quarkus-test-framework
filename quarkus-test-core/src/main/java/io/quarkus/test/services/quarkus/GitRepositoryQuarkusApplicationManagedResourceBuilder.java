package io.quarkus.test.services.quarkus;

import static java.util.regex.Pattern.quote;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ServiceLoader;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.services.GitRepositoryQuarkusApplication;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class GitRepositoryQuarkusApplicationManagedResourceBuilder extends ProdQuarkusApplicationManagedResourceBuilder {

    protected static final String QUARKUS_VERSION_PROPERTY = "${QUARKUS_VERSION}";

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
        return mavenArgs.replaceAll(quote(QUARKUS_VERSION_PROPERTY), QuarkusProperties.getVersion());
    }

    @Override
    public void init(Annotation annotation) {
        GitRepositoryQuarkusApplication metadata = (GitRepositoryQuarkusApplication) annotation;
        gitRepository = metadata.repo();
        gitBranch = metadata.branch();
        contextDir = metadata.contextDir();
        mavenArgs = metadata.mavenArgs();
        devMode = metadata.devMode();
        initAppClasses(new Class[0]);
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
}
