package io.quarkus.test.metrics;

import java.util.Optional;

import io.quarkus.test.bootstrap.ExtensionBootstrap;
import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.scenarios.QuarkusScenario;

public class MetricsExtensionBootstrap implements ExtensionBootstrap {

    private static final PropertyLookup METRICS_EXTENSION_ENABLED_PROPERTY = new PropertyLookup(
            "metrics.enabled", "true");
    private static final PropertyLookup METRICS_PUSH_AFTER_EACH_TEST = new PropertyLookup(
            "metrics.push-after-each-test", "false");

    private final boolean extensionEnabled;

    private QuarkusLabels metricCommonLabels;
    private QuarkusGauges quarkusGauges;
    private QuarkusHistograms quarkusHistograms;

    public MetricsExtensionBootstrap() {
        extensionEnabled = METRICS_EXTENSION_ENABLED_PROPERTY.getAsBoolean();
        if (extensionEnabled) {
            MetricsExporterService metricsExporterService = new MetricsExporterService();
            metricCommonLabels = new QuarkusLabels();
            quarkusGauges = new QuarkusGauges(metricsExporterService);
            quarkusHistograms = new QuarkusHistograms(metricsExporterService);
        }
    }

    @Override
    public boolean appliesFor(ScenarioContext context) {
        return extensionEnabled && context.isAnnotationPresent(QuarkusScenario.class);
    }

    @Override
    public void beforeAll(ScenarioContext context) {
        quarkusHistograms.startDurationBeforeAll(HistogramTypes.SCENARIO_TEST_TIME_SEC);
        metricCommonLabels.addModuleNameLabel();
        metricCommonLabels.addScenarioNameLabel(context.getRunningTestClassName());
        // Module success as default
        metricCommonLabels.markModuleAsSuccess();
    }

    @Override
    public void beforeEach(ScenarioContext context) {
        quarkusGauges.increment(GaugesTypes.TESTS_TOTAL);
        pushAfterEachTestIfEnabled();
    }

    @Override
    public void onSuccess(ScenarioContext context) {
        quarkusGauges.increment(GaugesTypes.TESTS_SUCCEED);
        pushAfterEachTestIfEnabled();
    }

    @Override
    public void onError(ScenarioContext context, Throwable throwable) {
        quarkusGauges.increment(GaugesTypes.TESTS_FAILED);
        metricCommonLabels.markModuleAsFailed();
        pushAfterEachTestIfEnabled();
    }

    @Override
    public void onDisabled(ScenarioContext context, Optional<String> reason) {
        quarkusGauges.increment(GaugesTypes.TESTS_IGNORE);
        pushAfterEachTestIfEnabled();
    }

    @Override
    public void afterAll(ScenarioContext context) {
        quarkusHistograms.stopDurationAfterAll(HistogramTypes.SCENARIO_TEST_TIME_SEC);
        quarkusHistograms.push(metricCommonLabels);
        if (!METRICS_PUSH_AFTER_EACH_TEST.getAsBoolean()) {
            quarkusGauges.push(metricCommonLabels);
        }
    }

    private void pushAfterEachTestIfEnabled() {
        if (METRICS_PUSH_AFTER_EACH_TEST.getAsBoolean()) {
            quarkusGauges.push(metricCommonLabels);
        }
    }
}
