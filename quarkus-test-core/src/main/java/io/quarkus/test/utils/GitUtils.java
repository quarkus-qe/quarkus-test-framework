package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import io.quarkus.test.bootstrap.ServiceContext;

public final class GitUtils {
    private static final String GIT = "git";
    private static final String CLONE = "clone";
    private static final String CHECKOUT = "checkout";

    private GitUtils() {

    }

    public static void cloneRepository(ServiceContext serviceContext, String repository) {
        try {
            new Command(Arrays.asList(GIT, CLONE, repository, "."))
                    .outputToConsole()
                    .onDirectory(serviceContext.getServiceFolder())
                    .runAndWait();

        } catch (Exception e) {
            fail("Failed to clone GIT repository " + repository + ". Caused by: " + e.getMessage());
        }
    }

    public static void checkoutBranch(ServiceContext serviceContext, String branch) {
        try {
            new Command(Arrays.asList(GIT, CHECKOUT, branch))
                    .outputToConsole()
                    .onDirectory(serviceContext.getServiceFolder())
                    .runAndWait();
        } catch (Exception e) {
            fail("Failed to checkout GIT branch " + branch + ". Caused by: " + e.getMessage());
        }
    }

    public static void showRepositoryState(ServiceContext serviceContext) {
        try {
            new Command(Arrays.asList(GIT, "log", "-n1", "--oneline", "--no-abbrev-commit"))
                    .outputToConsole()
                    .onDirectory(serviceContext.getServiceFolder())
                    .runAndWait();
        } catch (Exception e) {
            fail("Failed to show content of repository. Caused by: " + e.getMessage());
        }
    }
}
