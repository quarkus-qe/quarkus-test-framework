package io.quarkus.test.util;

import io.quarkus.test.bootstrap.QuarkusCliRestService;

/**
 * Producer of QuarkusCliRestService apps for tests in {@link QuarkusCLIUtils}.
 */
public interface IQuarkusCLIAppManager {
    /**
     * Create an app which can be updated.
     */
    default QuarkusCliRestService createApplication() {
        return createApplication((String) null);
    }

    /**
     * @param extensions Pass this parameter to
     *        {@link io.quarkus.test.bootstrap.QuarkusCliClient} createApplication.withExtensions
     */
    QuarkusCliRestService createApplication(String... extensions);

    /**
     * Update app to new quarkus version.
     */
    void updateApp(QuarkusCliRestService app);
}
