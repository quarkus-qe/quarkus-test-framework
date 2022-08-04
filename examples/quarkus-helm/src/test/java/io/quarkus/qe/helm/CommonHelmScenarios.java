package io.quarkus.qe.helm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.restassured.RestAssured;

public abstract class CommonHelmScenarios {

    protected abstract QuarkusHelmClient getHelmClient();

    @Inject
    static OpenShiftClient ocpClient;

    protected abstract String getPlatformName();

    @Test
    public void shouldInstallQuarkusAppThroughHelm() {
        QuarkusHelmClient helmClient = getHelmClient();
        String chartName = "examples-quarkus-helm";

        String chartFolderName = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/" + getPlatformName() + "/"
                + chartName;
        QuarkusHelmClient.Result chartResultCmd = helmClient.installChart(chartName, chartFolderName);
        thenSucceed(chartResultCmd);

        String appURL = ocpClient.url(chartName).getRestAssuredStyleUri();
        RestAssured.given().baseUri(appURL).get("/greeting")
                .then().statusCode(200)
                .body(is("Hello World!"));

        List<QuarkusHelmClient.ChartListResult> charts = helmClient.getCharts();
        assertTrue(charts.size() > 0, "Chart " + chartName + " not found. Installation fail");
        List<String> chartNames = charts.stream()
                .map(QuarkusHelmClient.ChartListResult::getName)
                .map(String::trim)
                .collect(Collectors.toList());
        assertThat(chartNames.toArray(), hasItemInArray(chartName));
    }

    @Test
    public void shouldInstallNewEmptyHelmChartManually() {
        QuarkusHelmClient helmClient = getHelmClient();
        String chartName = "mychart-manually";
        String chartFolderName = helmClient.getWorkingDirectory().getAbsolutePath() + "/" + chartName;
        QuarkusHelmClient.Result chartResultCmd = helmClient.run("create", chartName);
        thenSucceed(chartResultCmd);

        chartResultCmd = helmClient.run("install", chartName, chartFolderName);
        thenSucceed(chartResultCmd);
    }

    @Test
    public void shouldInstallNewEmptyHelmChartWithShortcuts() {
        QuarkusHelmClient helmClient = getHelmClient();
        String chartName = "mychart-shortcuts";
        QuarkusHelmClient.NewChartResult newChartResult = helmClient.createEmptyChart(chartName);
        thenSucceed(newChartResult);

        QuarkusHelmClient.Result chartResultCmd = helmClient.installChart(chartName, newChartResult.getChartFolderPath());
        thenSucceed(chartResultCmd);

        List<QuarkusHelmClient.ChartListResult> charts = helmClient.getCharts();
        assertTrue(charts.size() > 0, "Chart " + chartName + " not found. Installation fail");
        List<String> chartNames = charts.stream()
                .map(QuarkusHelmClient.ChartListResult::getName)
                .map(String::trim)
                .collect(Collectors.toList());
        assertThat(chartNames.toArray(), hasItemInArray(chartName));
    }

    @Test
    public void shouldUninstallHelmChart() {
        QuarkusHelmClient helmClient = getHelmClient();
        String chartName = "mychart-remove";
        QuarkusHelmClient.NewChartResult newChartResult = helmClient.createEmptyChart(chartName);
        thenSucceed(newChartResult);

        QuarkusHelmClient.Result chartResultCmd = helmClient.installChart(chartName, newChartResult.getChartFolderPath());
        thenSucceed(chartResultCmd);
        List<QuarkusHelmClient.ChartListResult> charts = helmClient.getCharts();
        List<String> chartNames = charts.stream()
                .map(QuarkusHelmClient.ChartListResult::getName)
                .map(String::trim)
                .collect(Collectors.toList());
        assertThat(chartNames.toArray(), hasItemInArray(chartName));

        chartResultCmd = helmClient.uninstallChart(chartName);
        thenSucceed(chartResultCmd);
        charts = helmClient.getCharts();
        chartNames = charts.stream().map(QuarkusHelmClient.ChartListResult::getName).map(String::trim)
                .collect(Collectors.toList());
        assertThat(chartNames, Matchers.not(contains(chartName)));
    }

    private void thenSucceed(QuarkusHelmClient.Result chartResultCmd) {
        assertTrue(
                chartResultCmd.isSuccessful(),
                String.format("Command %s fails", chartResultCmd.getCommandExecuted()));
    }
}
