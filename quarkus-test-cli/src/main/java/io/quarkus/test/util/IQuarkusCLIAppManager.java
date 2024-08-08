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
        return createApplicationWithExtensions((String) null);
    }

    /**
     * @param extensions Pass this parameter to
     *        {@link io.quarkus.test.bootstrap.QuarkusCliClient} createApplication.withExtensions
     */
    QuarkusCliRestService createApplicationWithExtensions(String... extensions);

    QuarkusCliRestService createApplicationWithExtraArgs(String... extraArgs);

    /**
     * Update app to new quarkus version.
     */
    void updateApp(QuarkusCliRestService app);
}
