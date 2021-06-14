package io.quarkus.test.metrics;

import java.util.Optional;

import org.gradle.internal.impldep.com.google.common.base.Strings;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.bootstrap.ExtensionBootstrap;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.scenarios.QuarkusScenario;

public class MetricsExtensionBootstrap implements ExtensionBootstrap {

    public static final String METRIC_FORCE_PUSH_TAG = "forcePush";

    private static final String PROMETHEUS_ENDPOINT_PROPERTY = "ts.prometheus-http-endpoint";
    private final boolean extensionEnabled;
    private  QuarkusGauges quarkusGauges;
    private  QuarkusHistograms quarkusHistograms;

    public MetricsExtensionBootstrap() {
        String prometheusHttpEndpoint = System.getProperty(PROMETHEUS_ENDPOINT_PROPERTY);
        extensionEnabled = !Strings.isNullOrEmpty(prometheusHttpEndpoint);
        if(extensionEnabled) {
            quarkusGauges = new QuarkusGauges(prometheusHttpEndpoint);
            quarkusHistograms = new QuarkusHistograms(prometheusHttpEndpoint);
        }
    }

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(QuarkusScenario.class);
    }

    @Override
    public void onSuccess(ExtensionContext context) {
        if(extensionEnabled) {
            quarkusGauges.upsert(GaugesTypes.TOTAL_SUCCESS);
            quarkusGauges.upsert(GaugesTypes.TOTAL);
            quarkusGauges.upsert(GaugesTypes.MODULE_SUCCESS);
            onlyIfRequiredForcePush(context);
        }
    }

    @Override
    public void onError(ExtensionContext context, Throwable throwable) {
        if(extensionEnabled) {
            quarkusGauges.upsert(GaugesTypes.TOTAL_FAIL);
            quarkusGauges.upsert(GaugesTypes.TOTAL);
            quarkusGauges.upsert(GaugesTypes.MODULE_FAIL);
            onlyIfRequiredForcePush(context);
        }
    }

    @Override
    public void onDisabled(ExtensionContext context, Optional<String> reason) {
        if(extensionEnabled) {
            quarkusGauges.upsert(GaugesTypes.TOTAL_IGNORE);
            quarkusGauges.upsert(GaugesTypes.TOTAL);
            quarkusGauges.upsert(GaugesTypes.MODULE_IGNORE);
            onlyIfRequiredForcePush(context);
        }
    }

    @Override
    public void onServiceStarted(ExtensionContext context, Service service) {
    }

    @Override
    public void onServiceInitiate(ExtensionContext context, Service service) {
    }

    @Override
    public void beforeEach(ExtensionContext context) {
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if(extensionEnabled) {
            quarkusHistograms.startDurationBeforeAll(HistogramTypes.MODULE_TEST_TIME_SEC);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if(extensionEnabled) {
            quarkusHistograms.stopDurationAfterAll(HistogramTypes.MODULE_TEST_TIME_SEC);
            quarkusHistograms.push();
            quarkusGauges.push();
        }
    }

    private static boolean checkForcePush(String tag) {
        return tag.equalsIgnoreCase(METRIC_FORCE_PUSH_TAG);
    }

    private void onlyIfRequiredForcePush(ExtensionContext context) {
        context.getTags().stream()
                .filter(MetricsExtensionBootstrap::checkForcePush)
                .findFirst().ifPresent(requiredForcePush -> quarkusGauges.push());
    }
}
