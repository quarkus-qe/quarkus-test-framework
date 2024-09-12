package io.quarkus.test.util;

import static io.quarkus.test.bootstrap.QuarkusCliClient.UpdateApplicationRequest.defaultUpdate;
import static io.quarkus.test.util.QuarkusCLIUtils.getQuarkusAppVersion;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.logging.Log;

/**
 * AppManager designed to update app to specific quarkus-bom version, instead of to stream as DefaultQuarkusCLIAppManager.
 */
public class RHBQPlatformAppManager extends DefaultQuarkusCLIAppManager {
    protected final DefaultArtifactVersion newPlatformVersion;

    public RHBQPlatformAppManager(QuarkusCliClient cliClient, DefaultArtifactVersion oldStreamVersion,
            DefaultArtifactVersion newStreamVersion, DefaultArtifactVersion newPlatformVersion) {
        super(cliClient, oldStreamVersion, newStreamVersion);
        this.newPlatformVersion = newPlatformVersion;
    }

    @Override
    public void updateApp(QuarkusCliRestService app) {
        Log.info("Updating app to version: " + newPlatformVersion);
        app.update(defaultUpdate()
                .withPlatformVersion(newPlatformVersion.toString()));
    }

    @Override
    public QuarkusCliRestService createApplication() {
        return assertIsUsingRHBQ(super.createApplication());
    }

    @Override
    public QuarkusCliRestService createApplicationWithExtensions(String... extensions) {
        return assertIsUsingRHBQ(super.createApplicationWithExtensions(extensions));
    }

    @Override
    public QuarkusCliRestService createApplicationWithExtraArgs(String... extraArgs) {
        return assertIsUsingRHBQ(super.createApplicationWithExtraArgs(extraArgs));
    }

    private QuarkusCliRestService assertIsUsingRHBQ(QuarkusCliRestService app) {
        try {
            // check that created application uses version with "redhat" suffix.
            assertTrue(getQuarkusAppVersion(app).toString().contains("redhat"),
                    "Created Quarkus application does not use \"redhat\" version, while should be RHBQ");
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
        return app;
    }
}
