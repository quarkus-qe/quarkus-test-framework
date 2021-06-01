package io.quarkus.test.services.quarkus;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

public class GitRepositoryQuarkusApplicationManagedResourceBuilder extends QuarkusApplicationManagedResourceBuilder {

    private final ServiceLoader<GitRepositoryQuarkusApplicationManagedResourceBinding> bindings = ServiceLoader
            .load(GitRepositoryQuarkusApplicationManagedResourceBinding.class);

    private String gitRepository;
    private String gitBranch;
    private String contextDir;
    private String mavenArgs;
    private QuarkusManagedResource managedResource;

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

    @Override
    public void init(Annotation annotation) {
        GitRepositoryQuarkusApplication metadata = (GitRepositoryQuarkusApplication) annotation;
        gitRepository = metadata.repo();
        gitBranch = metadata.branch();
        contextDir = metadata.contextDir();
        mavenArgs = metadata.mavenArgs();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
        configureLogging();
        managedResource = findManagedResource();
        build();

        managedResource.validate();

        return managedResource;
    }

    @Override
    protected void build() {

    }

    private QuarkusManagedResource findManagedResource() {
        for (GitRepositoryQuarkusApplicationManagedResourceBinding binding : bindings) {
            if (binding.appliesFor(getContext())) {
                return binding.init(this);
            }
        }

        Assertions.fail("No managed resource builder found for @GitRepositoryQuarkusApplication annotation");
        return null;
    }
}
