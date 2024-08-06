package io.quarkus.test.util;

import static io.quarkus.test.bootstrap.QuarkusCliClient.CreateApplicationRequest.defaults;
import static io.quarkus.test.bootstrap.QuarkusCliClient.UpdateApplicationRequest.defaultUpdate;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.logging.Log;
import io.quarkus.test.services.quarkus.CliDevModeVersionLessQuarkusApplicationManagedResource;

public class DefaultQuarkusCLIAppManager implements IQuarkusCLIAppManager {
    private final QuarkusCliClient cliClient;
    private final DefaultArtifactVersion oldVersion;
    private final DefaultArtifactVersion newVersion;

    public DefaultQuarkusCLIAppManager(QuarkusCliClient cliClient,
            DefaultArtifactVersion oldVersion, DefaultArtifactVersion newVersion) {
        this.cliClient = cliClient;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    @Override
    public void updateApp(QuarkusCliRestService app) {
        Log.info("Updating app to version stream: " + newVersion);
        app.update(defaultUpdate().withStream(newVersion.toString()));
    }

    @Override
    public QuarkusCliRestService createApplication() {
        Log.info("Creating app with version stream: " + oldVersion);
        return cliClient.createApplication("app", defaults()
                .withPlatformBom(null)
                .withStream(oldVersion.toString())
                // overwrite managedResource to use quarkus version defined in pom.xml and not overwrite it in CLI command
                .withManagedResourceCreator((serviceContext,
                        quarkusCliClient) -> managedResBuilder -> new CliDevModeVersionLessQuarkusApplicationManagedResource(
                                serviceContext, quarkusCliClient)));
    }

    @Override
    public QuarkusCliRestService createApplication(String... extensions) {
        Log.info("Creating app with version stream: " + oldVersion + " and extensions " + extensions);
        return cliClient.createApplication("app", defaults()
                .withPlatformBom(null)
                .withExtensions(extensions)
                .withStream(oldVersion.toString())
                // overwrite managedResource to use quarkus version defined in pom.xml and not overwrite it in CLI command
                .withManagedResourceCreator((serviceContext,
                        quarkusCliClient) -> managedResBuilder -> new CliDevModeVersionLessQuarkusApplicationManagedResource(
                                serviceContext, quarkusCliClient)));
    }
}
