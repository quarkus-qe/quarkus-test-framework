package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings.usingTimeout;
import static java.time.Duration.ofSeconds;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class DevModeQuarkusService extends RestService {

    private static final int WAITING_TIMEOUT_SEC = 15;
    private static final String DEV_UI_CONTINUOUS_TESTING_PATH = "/q/dev-ui/continuous-testing";
    private static final String START_CONTINUOUS_TESTING_BTN_CSS_ID = "#start-continuous-testing-btn";
    private static final String NO_TESTS_FOUND = "No tests found";
    private static final String TESTS_PAUSED = "Tests paused";
    private static final String TESTS_ARE_PASSING = "tests are passing";
    private static final String TESTS_IS_PASSING = "test is passing";
    private static final String RUNNING_TESTS_FOR_1ST_TIME = "Running tests for the first time";
    /**
     * Following hooks are currently logged by {@link io.quarkus.deployment.dev.testing.TestConsoleHandler}.
     * They should only be present if {@link #RUNNING_TESTS_FOR_1ST_TIME} is also logged, but testing for all of them
     * make detection of the state of the continuous testing less error-prone.
     */
    private static final Set<String> CONTINUOUS_TESTING_ENABLED_HOOKS = Set.of("Starting tests",
            "Starting test run", NO_TESTS_FOUND, TESTS_IS_PASSING, TESTS_ARE_PASSING,
            "All tests are now passing");

    /**
     * Enables continuous testing if and only if it wasn't enabled already (no matter what the current state is)
     * and there are no tests to run or all tests are passing. Logic required for second re-enabling of continuous
     * testing is more robust, and we don't really need it. Same goes for scenario when testing is enabled and tests fail.
     *
     * @return DevModeQuarkusService
     */
    public DevModeQuarkusService enableContinuousTesting() {

        // check if continuous testing is disabled
        if (testsArePaused()) {

            // go to 'continuous-testing' page and click on 'Start' button which enables continuous testing
            try (Playwright playwright = Playwright.create()) {
                try (Browser browser = playwright.chromium().launch()) {
                    Page page = browser.newContext().newPage();
                    page.navigate(getContinuousTestingPath());
                    var locatorOptions = new Page.LocatorOptions();
                    locatorOptions.setHasText("Start");
                    page.locator(START_CONTINUOUS_TESTING_BTN_CSS_ID, locatorOptions).click();
                    // wait till enabling of continuous testing is finished
                    untilAsserted(() -> logs().assertContains(NO_TESTS_FOUND, TESTS_IS_PASSING, TESTS_ARE_PASSING),
                            usingTimeout(ofSeconds(WAITING_TIMEOUT_SEC)));
                }
            }
        }

        return this;
    }

    private String getContinuousTestingPath() {
        return getURI(Protocol.HTTP).withPath(DEV_UI_CONTINUOUS_TESTING_PATH).toString();
    }

    private boolean testsArePaused() {
        boolean testsArePaused = false;
        for (String entry : getLogs()) {
            if (entry.contains(TESTS_PAUSED)) {
                testsArePaused = true;
                // we intentionally continue looking as we need to be sure testing wasn't enabled in past
                continue;
            }

            if (entry.contains(RUNNING_TESTS_FOR_1ST_TIME)) {
                // continuous testing is already enabled
                return false;
            }

            if (CONTINUOUS_TESTING_ENABLED_HOOKS.stream().anyMatch(entry::contains)) {
                throw new IllegalStateException(String.format(
                        "Implementation of continuous testing in Quarkus application has changed as we detected "
                                + "'%s' log message, but message '%s' wasn't logged",
                        entry, RUNNING_TESTS_FOR_1ST_TIME));
            }
        }

        if (testsArePaused) {
            // continuous testing disabled
            return true;
        }

        // we only get here if implementation has changed (e.g. hooks are different now),
        // or there is a bug in continuous testing
        throw new IllegalStateException("State of continuous testing couldn't be recognized");
    }

    public void modifyFile(String file, Function<String, String> modifier) {
        try {
            File targetFile = getServiceFolder().resolve(file).toFile();
            String original = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
            String updated = modifier.apply(original);

            FileUtils.writeStringToFile(targetFile, updated, StandardCharsets.UTF_8, false);
        } catch (IOException e) {
            Assertions.fail("Error modifying file. Caused by " + e.getMessage());
        }
    }

    public void copyFile(String file, String target) {
        try {
            Path sourcePath = Path.of(file);
            File targetPath = getServiceFolder().resolve(target).toFile();
            FileUtils.deleteQuietly(targetPath);

            FileUtils.copyFile(sourcePath.toFile(), targetPath);
            if (!targetPath.setLastModified(System.currentTimeMillis())) {
                throw new IllegalStateException("Failed to set the last-modified time of the file: " + targetPath.getPath());
            }
        } catch (IOException e) {
            Assertions.fail("Error copying file. Caused by " + e.getMessage());
        }
    }

}
