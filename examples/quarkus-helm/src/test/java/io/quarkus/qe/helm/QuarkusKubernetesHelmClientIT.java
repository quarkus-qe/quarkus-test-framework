package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.FileNotFoundException;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@QuarkusScenario
@DisabledOnNative // Helm is concerned just about image name, Native compilation is not relevant
public class QuarkusKubernetesHelmClientIT {

    @Inject
    static QuarkusHelmClient helmClient;

    @Test
    public void verifyChartValuesYamlContent() throws FileNotFoundException {
        String chartName = "examples-quarkus-helm";
        String chartFolderName = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/kubernetes/" + chartName;
        Map<String, String> values = (Map<String, String>) helmClient.getChartValues(chartFolderName).get("app");
        assertFalse(values.get("image").isEmpty(), "Chart values.yaml host should not be empty");
    }

    @Test
    public void verifyIngressExist() throws FileNotFoundException {
        String chartName = "examples-quarkus-helm";
        String templates = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/kubernetes/" + chartName + "/templates";
        assertDoesNotThrow(() -> helmClient.getRawYaml("ingress.yaml", templates), "Ingress not generated");
    }

    @Test
    public void verifyDeploymentExist() {
        String chartName = "examples-quarkus-helm";
        String templates = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/kubernetes/" + chartName + "/templates";
        assertDoesNotThrow(() -> helmClient.getRawYaml("deployment.yaml", templates), "Deployment not generated");
    }

    @Test
    public void verifyServiceExist() {
        String chartName = "examples-quarkus-helm";
        String templates = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/kubernetes/" + chartName + "/templates";
        assertDoesNotThrow(() -> helmClient.getRawYaml("service.yaml", templates), "Service not generated");
    }
}
