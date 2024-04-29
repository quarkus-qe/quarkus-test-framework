package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;

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
        configureCertificates();
        if (QuarkusProperties.disableBuildAnalytics()) {
            getContext()
                    .getOwner()
                    .withProperty(QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY, Boolean.TRUE.toString());
        }
        build();
        return new DevModeLocalhostQuarkusApplicationManagedResource(this);
    }

    protected void build() {
        try {
            FileUtils.copyCurrentDirectoryTo(getContext().getServiceFolder());
            copyResourcesToAppFolder();
        } catch (Exception ex) {
            fail("Failed to build Quarkus artifacts. Caused by " + ex);
        }
    }
}
