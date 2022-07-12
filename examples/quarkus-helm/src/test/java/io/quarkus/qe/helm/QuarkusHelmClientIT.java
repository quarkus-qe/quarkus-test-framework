package io.quarkus.qe.helm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@Tag("quarkus-helm")
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Quarkus Helm not supported")
// TODO https://github.com/quarkiverse/quarkus-helm/issues/29
@DisabledOnQuarkusSnapshot(reason = "unsupported quarkus-helm/dekorate version")
public class QuarkusHelmClientIT {

    private final static String EXPECTED_HELM_VERSION_REGEXP = ".*[v]{1}\\d{1,2}\\.\\d{1,2}\\.\\d{1,3}.*";
    private final static String EXPECTED_HOST = "examples-quarkus-helm.apps.ocp4-10.dynamic.quarkus";

    @Inject
    static QuarkusHelmClient helmClient;

    @Test
    public void verifyHelmInjection() {
        QuarkusHelmClient.Result helmCmdResult = helmClient.run("version");
        assertTrue(
                helmCmdResult.isSuccessful(),
                String.format("Command %s fails", helmCmdResult.getCommandExecuted()));
        assertTrue(
                helmCmdResult.getOutput().matches(EXPECTED_HELM_VERSION_REGEXP),
                "Unexpected helm version");
    }

    @Test
    public void verifyChartValuesYamlContent() throws FileNotFoundException {
        String chartName = "examples-quarkus-helm";
        String chartFolderName = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/" + chartName;
        Map<String, String> values = (Map<String, String>) helmClient.getChartValues(chartName, chartFolderName)
                .get("examplesQuarkusHelm");
        assertEquals(values.get("host"), EXPECTED_HOST, "Unexpected Chart values.yaml host");
        assertFalse(values.get("image").isEmpty(), "Chart values.yaml host should not be empty");
    }
}
