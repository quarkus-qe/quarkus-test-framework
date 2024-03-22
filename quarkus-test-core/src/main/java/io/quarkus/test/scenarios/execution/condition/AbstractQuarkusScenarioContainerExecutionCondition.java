package io.quarkus.test.scenarios.execution.condition;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.join;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.utils.Command;

public abstract class AbstractQuarkusScenarioContainerExecutionCondition implements QuarkusScenarioExecutionCondition {

    public static final ConditionEvaluationResult ENV_SUPPORTS_LINUX_CONTAINERS = enabled("Environment supports"
            + " Linux containers");
    public static final String ENV_DOES_NOT_SUPPORT_LINUX_CONTAINERS = "Test class '%s' requires Linux containers, "
            + "but the environment does not support them";
    static final ConditionEvaluationResult CONDITION_NOT_MATCHED = enabled("This condition should "
            + "only be applied on test classes annotated with the '@QuarkusScenario' annotation");
    private static final Logger LOG = Logger.getLogger(AbstractQuarkusScenarioContainerExecutionCondition.class.getName());
    private static final String LINUX_CONTAINERS_NOT_REQUIRED = "Test class '%s' does not require containers";
    private static final String LINUX_CONTAINER_OS_TYPE = "linux";
    private static final String PODMAN = "podman";
    private static final String DOCKER_HOST = "DOCKER_HOST";
    private static final PropertyLookup DOCKER_DETECTION_ENABLED = new PropertyLookup("ts.docker-detection-enabled", "true");
    private static Boolean areLinuxContainersSupported = null;

    @Override
    public final ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (TRUE.equals(areLinuxContainersSupported)) {
            // optimization - don't evaluate condition if we know Linux containers are available or evaluation is disabled
            return ENV_SUPPORTS_LINUX_CONTAINERS;
        }

        // Docker detection implemented here leverage Docker CLI, however TestContainers does not require the CLI to work
        if (!DOCKER_DETECTION_ENABLED.getAsBoolean()) {
            areLinuxContainersSupported = TRUE;
            return ENV_SUPPORTS_LINUX_CONTAINERS;
        }

        return context
                .getElement()
                .filter(AbstractQuarkusScenarioContainerExecutionCondition::isQuarkusScenario)
                .map(clazz -> (Class<?>) clazz)
                .map(this::evaluateExecutionCondition)
                .orElse(CONDITION_NOT_MATCHED);
    }

    private ConditionEvaluationResult evaluateExecutionCondition(Class<?> testClass) {
        if (areContainersRequired(testClass)) {
            if (areLinuxContainersSupported()) {
                return ENV_SUPPORTS_LINUX_CONTAINERS;
            }
            return disabled(format(ENV_DOES_NOT_SUPPORT_LINUX_CONTAINERS, testClass));
        }
        return enabled(format(LINUX_CONTAINERS_NOT_REQUIRED, testClass));
    }

    protected abstract boolean areContainersRequired(Class<?> testClass);

    public static synchronized boolean areLinuxContainersSupported() {
        if (areLinuxContainersSupported == null) {
            areLinuxContainersSupported = checkLinuxContainersSupported();
        }
        return areLinuxContainersSupported;
    }

    private static boolean checkLinuxContainersSupported() {
        return isPodman() || dockerIsUsingLinuxContainers();
    }

    private static boolean dockerIsUsingLinuxContainers() {
        final var osType = runDockerOsTypeInfoCommand();
        return osType
                .stream()
                .filter(str -> str.contains(LINUX_CONTAINER_OS_TYPE))
                .findAny()
                .map(str -> TRUE)
                .orElseGet(() -> {
                    LOG.debugf("Linux containers are required, but container type is: '%s'", join(" ", osType));
                    return FALSE;
                });
    }

    private static boolean isPodman() {
        final String dockerHostEnvVar = System.getenv(DOCKER_HOST);
        if (dockerHostEnvVar != null && dockerHostEnvVar.contains(PODMAN)) {
            return true;
        }

        try {
            var output = new ArrayList<String>();
            new Command("docker", "--version").outputToLines(output).runAndWait();
            return output.stream().anyMatch(line -> line.contains(PODMAN));
        } catch (Exception e) {
            LOG.error("Failed to check whether Podman is used: ", e);
            return false;
        }
    }

    static boolean isQuarkusScenario(AnnotatedElement annotatedElement) {
        return annotatedElement.isAnnotationPresent(QuarkusScenario.class);
    }

    private static Collection<String> runDockerOsTypeInfoCommand() {
        try {
            var output = new ArrayList<String>();
            new Command("docker", "info", "--format", "'{{.OSType}}'")
                    .outputToLines(output)
                    .runAndWait();
            return output;
        } catch (Exception e) {
            LOG.error("Failed to check whether Linux containers are supported: ", e);
            return List.of();
        }
    }

}
