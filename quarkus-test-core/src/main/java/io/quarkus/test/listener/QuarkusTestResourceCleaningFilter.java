package io.quarkus.test.listener;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.isNativeEnabled;
import static java.lang.Boolean.TRUE;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.services.quarkus.QuarkusMavenPluginBuildHelper;

/**
 * Deletes native executables we know that are no longer necessary because all the test classes inside module
 * finished testing. User can opt-out with {@link Configuration.Property.DELETE_FOLDER_ON_EXIT} flag.
 */
public final class QuarkusTestResourceCleaningFilter implements TestExecutionListener {

    private static final PropertyLookup DELETE_SERVICE_FOLDER = new PropertyLookup(
            Configuration.Property.DELETE_FOLDER_ON_EXIT.getName(),
            TRUE.toString());

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (DELETE_SERVICE_FOLDER.getAsBoolean() && isNativeEnabled()) {
            QuarkusMavenPluginBuildHelper.deleteNativeExecutablesInPermanentLocation();
        }
    }
}
