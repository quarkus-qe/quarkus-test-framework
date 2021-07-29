package io.quarkus.test.metrics;

import static io.quarkus.test.metrics.QuarkusLabels.MODULE_STATUS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;

public class QuarkusGauges {

    private static final Logger LOG = Logger.getLogger(MetricsExtensionBootstrap.class);

    private final PushGateway prometheusClient;
    private final Map<String, Gauge> gaugesBucket = new HashMap<>();
    private final CollectorRegistry defaultRegistry;
    private final Map<GaugesTypes, CollectorRegistry> modulesRegistry = new HashMap<>();
    private final QuarkusLabels labels;

    public QuarkusGauges(String prometheusHttpEndpoint) {
        labels = new QuarkusLabels();
        prometheusClient = new PushGateway(prometheusHttpEndpoint);
        modulesRegistry.put(GaugesTypes.MODULE_SUCCESS, new CollectorRegistry());
        modulesRegistry.put(GaugesTypes.MODULE_FAIL, new CollectorRegistry());
        modulesRegistry.put(GaugesTypes.MODULE_IGNORE, new CollectorRegistry());
        defaultRegistry = new CollectorRegistry();
    }

    public void upsert(GaugesTypes gaugesTypes) {
        String gaugeBucketID = getGaugeBucketID(gaugesTypes);
        createGaugeIfNotExist(gaugesTypes, gaugeBucketID);
        labels.addModuleNameLabel();
        gaugesBucket.get(gaugeBucketID).inc();
    }

    private void createGaugeIfNotExist(GaugesTypes gaugesTypes, String gaugeBucketID) {
        if (!gaugesBucket.containsKey(gaugeBucketID)) {
            CollectorRegistry registry = Optional.ofNullable(modulesRegistry.get(gaugesTypes)).orElse(defaultRegistry);
            Gauge gaugeModule = Gauge.build()
                    .name(gaugesTypes.getCode()).help("Module gauge")
                    .register(registry);
            gaugesBucket.put(gaugeBucketID, gaugeModule);
        }
    }

    public void push() {
        try {
            prometheusClient.pushAdd(defaultRegistry, labels.getServiceName(), labels.getLabelsBucket());

            for (Map.Entry<GaugesTypes, CollectorRegistry> registry : modulesRegistry.entrySet()) {
                Map<String, String> registryLabels = labels.getLabelsBucket();
                registryLabels.put(MODULE_STATUS, registry.getKey().name().toLowerCase());
                prometheusClient.pushAdd(registry.getValue(), labels.getServiceName(), registryLabels);
            }

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private String getGaugeBucketID(GaugesTypes gaugesTypes) {
        return gaugesTypes.getCode() + "_" + gaugesTypes;
    }
}
