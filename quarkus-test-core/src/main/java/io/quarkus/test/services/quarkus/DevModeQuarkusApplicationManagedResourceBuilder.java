package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.utils.FileUtils;

public class DevModeQuarkusApplicationManagedResourceBuilder extends QuarkusApplicationManagedResourceBuilder {

    @Override
    public void init(Annotation annotation) {
        DevModeQuarkusApplication metadata = (DevModeQuarkusApplication) annotation;
        initAppClasses(metadata.classes());
        setPropertiesFile(metadata.properties());
        setGrpcEnabled(metadata.grpc());
    }

    @Override
    protected Path getResourcesApplicationFolder() {
        return super.getResourcesApplicationFolder().resolve(RESOURCES_FOLDER);
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        setContext(context);
        configureLogging();
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
