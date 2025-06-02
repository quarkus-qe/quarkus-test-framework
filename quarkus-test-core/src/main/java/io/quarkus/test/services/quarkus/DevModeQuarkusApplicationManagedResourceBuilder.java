package io.quarkus.test.services.quarkus;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.security.certificate.QuarkusApplicationCertificateConfigurator;
import io.quarkus.test.services.DevModeQuarkusApplication;

public class DevModeQuarkusApplicationManagedResourceBuilder extends QuarkusApplicationManagedResourceBuilder {

    @Override
    public void init(Annotation annotation) {
        DevModeQuarkusApplication metadata = (DevModeQuarkusApplication) annotation;
        initAppClasses(metadata.classes());
        setPropertiesFile(metadata.properties());
        setGrpcEnabled(metadata.grpc());
        setSslEnabled(metadata.ssl());
        setCertificateBuilder(CertificateBuilder.of(metadata.certificates()));
    }

    @Override
    protected Path getResourcesApplicationFolder() {
        return super.getResourcesApplicationFolder().resolve(RESOURCES_FOLDER);
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
        configureLogging();
        if (certificateBuilder != null) {
            QuarkusApplicationCertificateConfigurator.configureCertificates(certificateBuilder, getContext());
        }
        build();
        return new DevModeLocalhostQuarkusApplicationManagedResource(this);
    }

    protected void build() {
        new QuarkusMavenPluginBuildHelper(this).prepareApplicationFolder();
    }
}
