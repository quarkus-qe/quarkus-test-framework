package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@QuarkusScenario
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
public class QuarkusOpenShiftHelmClientIT {

    @Inject
    static QuarkusHelmClient helmClient;

    @Test
    @EnabledIf(value = "io.quarkus.test.bootstrap.HelmUtils#isHelmInstalled", disabledReason = "Helm needs to be locally installed")
    public void verifyHelmInjection() {
        QuarkusHelmClient.Result helmCmdResult = helmClient.run("version");
        assertTrue(
                helmCmdResult.isSuccessful(),
                String.format("Command %s fails", helmCmdResult.getCommandExecuted()));
        assertFalse(
                helmCmdResult.getOutput().isEmpty(),
                "Unexpected helm version");
    }

    @Test
    public void verifyChartValuesYamlContent() throws FileNotFoundException {
        String chartName = "examples-quarkus-helm";
        String chartFolderName = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/openshift/" + chartName;
        Map<String, String> values = (Map<String, String>) helmClient.getChartValues(chartFolderName).get("app");
        assertFalse(values.get("image").isEmpty(), "Chart values.yaml host should not be empty");
    }

    @Test
    public void verifyRouteExist() {
        String chartName = "examples-quarkus-helm";
        String templates = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/openshift/" + chartName + "/templates";
        assertDoesNotThrow(() -> helmClient.getRawYaml("route.yaml", templates), "Route not generated");
    }

    @Test
    public void verifyDeploymentExist() {
        String chartName = "examples-quarkus-helm";
        String templates = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/openshift/" + chartName + "/templates";
        assertDoesNotThrow(() -> helmClient.getRawYaml("deploymentconfig.yaml", templates), "Deployment config not generated");
    }

    @Test
    public void verifyServiceExist() {
        String chartName = "examples-quarkus-helm";
        String templates = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/openshift/" + chartName + "/templates";
        assertDoesNotThrow(() -> helmClient.getRawYaml("service.yaml", templates), "Service not generated");
    }
}
