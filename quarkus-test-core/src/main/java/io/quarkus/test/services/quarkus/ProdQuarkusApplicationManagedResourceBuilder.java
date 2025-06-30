package io.quarkus.test.services.quarkus;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.services.QuarkusApplication;

public class ProdQuarkusApplicationManagedResourceBuilder extends ArtifactQuarkusApplicationManagedResourceBuilder {

    static final String TARGET = "target";

    private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(QuarkusApplicationManagedResourceBinding.class);

    private Path artifact;
    private QuarkusManagedResource managedResource;
    private String artifactSuffix;

    @Override
    protected Path getArtifact() {
        return artifact;
    }

    protected String getArtifactSuffix() {
        return artifactSuffix;
    }

    protected void setArtifactSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty() || suffix.isBlank()) {
            this.artifactSuffix = null;
        } else {
            this.artifactSuffix = suffix;
        }
    }

    @Override
    public void init(Annotation annotation) {
        QuarkusApplication metadata = (QuarkusApplication) annotation;
        setPropertiesFile(metadata.properties());
        setSslEnabled(metadata.ssl());
        setGrpcEnabled(metadata.grpc());
        initAppClasses(metadata.classes());
        initForcedDependencies(metadata.dependencies());
        setOcpTlsPort(metadata.ocpTlsPort());
        setCertificateBuilder(CertificateBuilder.of(metadata.certificates()));
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
        configureLogging();
        managedResource = findManagedResource();
        build();

        return managedResource;
    }

    public void build() {
        managedResource.onPreBuild();
        copyResourcesToAppFolder();
        if (managedResource.needsBuildArtifact()) {
            this.artifact = tryToReuseOrBuildArtifact();
        }

        managedResource.onPostBuild();
    }

    protected QuarkusManagedResource findManagedResource() {
        for (QuarkusApplicationManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(getContext())) {
                return binding.init(this);
            }
        }

        return new ProdLocalhostQuarkusApplicationManagedResource(this);
    }

    protected Path getTargetFolderForLocalArtifacts() {
        return Paths.get(TARGET);
    }

    private Path tryToReuseOrBuildArtifact() {
        return new QuarkusMavenPluginBuildHelper(this, getTargetFolderForLocalArtifacts(), artifactSuffix)
                .buildOrReuseArtifact();
    }

}
