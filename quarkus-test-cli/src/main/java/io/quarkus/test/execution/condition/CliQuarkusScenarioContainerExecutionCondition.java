package io.quarkus.test.execution.condition;

import io.quarkus.test.scenarios.execution.condition.AbstractQuarkusScenarioContainerExecutionCondition;

/**
 * Disables Quarkus CLI tests when Linux containers are not available as for now, we are not able to recognize
 * whether unit test invokes DEV mode or not.
 */
public class CliQuarkusScenarioContainerExecutionCondition
        extends AbstractQuarkusScenarioContainerExecutionCondition {

    private static final String QUARKUS_CLI_TEST_CLASS_PREFIX = "QuarkusCli";

    @Override
    protected boolean areContainersRequired(Class<?> testClass) {
        // TODO: we can run cli tests that are not DEV mode tests on Windows, but first we need to revise
        //   io.quarkus.test.bootstrap.QuarkusCliClient.createApplication that launches app in a DEV mode
        //   and then find a mechanism that detects whether app is run in a DEV mode (like Assumption or tag)
        return testClass.getSimpleName().startsWith(QUARKUS_CLI_TEST_CLASS_PREFIX);
    }

}
