package io.quarkus.qe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.util.DefaultQuarkusCLIAppManager;
import io.quarkus.test.util.IQuarkusCLIAppManager;
import io.quarkus.test.util.QuarkusCLIUtils;

/**
 * Example how {@link QuarkusCLIUtils#checkDependenciesUpdate} and {@link IQuarkusCLIAppManager} can be used.
 * This class actually creates a quarkus app on stream 3.2, updates it to 3.8 and does an example test.
 */
@Tag("quarkus-cli")
@QuarkusScenario
public class QuarkusCliUtilsExampleIT {
    private static final DefaultArtifactVersion oldVersion = new DefaultArtifactVersion("3.2");
    private static final DefaultArtifactVersion newVersion = new DefaultArtifactVersion("3.8");
    private final IQuarkusCLIAppManager appManager;
    @Inject
    static QuarkusCliClient cliClient;

    public QuarkusCliUtilsExampleIT() {
        this.appManager = new DefaultQuarkusCLIAppManager(cliClient, oldVersion, newVersion);
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/42567")
    public void exampleDependencyUpdateTest() throws XmlPullParserException, IOException {
        List<Dependency> oldDependencies = new ArrayList<>();
        oldDependencies.add(new QuarkusCLIUtils.QuarkusDependency("io.quarkus:quarkus-rest-client"));

        List<Dependency> newDependencies = new ArrayList<>();
        newDependencies.add(new QuarkusCLIUtils.QuarkusDependency("io.quarkus:quarkus-resteasy-client"));

        QuarkusCLIUtils.checkDependenciesUpdate(appManager, oldDependencies, newDependencies);
    }
}
