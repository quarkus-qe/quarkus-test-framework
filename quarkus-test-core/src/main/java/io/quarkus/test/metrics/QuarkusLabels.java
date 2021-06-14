package io.quarkus.test.metrics;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.builder.Version;
import io.quarkus.test.configuration.PropertyLookup;

public class QuarkusLabels {

    public static final String MODULE_STATUS = "ts_quarkus_module_status";
    private static final String DEFAULT_SERVICE_NAME = "quarkus_test_framework";
    private static final String DEFAULT_BUILD_NUMBER = "777-default";
    private static final String DEFAULT_VERSION_NUMBER = "999-default";
    private static final String PLATFORM_LABEL_NAME = "ts_quarkus_platform";
    private static final String PLUGIN_VERSION_LABEL_NAME = "ts_quarkus_plugin_version";
    private static final String SERVICE_NAME_LABEL_NAME = "ts_quarkus_service_name";
    private static final String BUILD_NUMBER_LABEL_NAME = "ts_quarkus_build_number";

    private final String serviceName;
    private final String quarkusPluginVersion;
    private final String buildNumber;
    private final Map<String, String> labelsBucket = new HashMap<>();

    public QuarkusLabels() {
        serviceName = new PropertyLookup("ts.service-name", DEFAULT_SERVICE_NAME).get();
        quarkusPluginVersion = new PropertyLookup("quarkus-plugin.version", Version.getVersion()).get();
        buildNumber = new PropertyLookup("ts.buildNumber", DEFAULT_BUILD_NUMBER).get();
        String versionNumber = new PropertyLookup("ts.versionNumber", quarkusPluginVersion).get();

        labelsBucket.put(BUILD_NUMBER_LABEL_NAME, buildNumber);
        labelsBucket.put(PLUGIN_VERSION_LABEL_NAME, versionNumber);
        labelsBucket.put(SERVICE_NAME_LABEL_NAME, serviceName);

        addPlatformLabel();
    }

    private void addPlatformLabel() {
        String openshift = new PropertyLookup("openshift", "").get();
        String k8s = new PropertyLookup("kubernetes", "").get();
        if (!openshift.isEmpty()) {
            labelsBucket.put(PLATFORM_LABEL_NAME, "ocp");
        }

        if (!k8s.isEmpty()) {
            labelsBucket.put(PLATFORM_LABEL_NAME, "k8s");
        }

        if (openshift.isEmpty() && k8s.isEmpty()) {
            labelsBucket.put(PLATFORM_LABEL_NAME, "bare-metal");
        }
    }

    public void addLabel(String labelName, String labelValue) {
        if (!labelsBucket.containsKey(labelName)) {
            labelsBucket.put(labelName, labelValue);
        }
    }

    private String getModuleName() {
        String userDirectory = System.getProperty("user.dir");
        return userDirectory.substring(userDirectory.lastIndexOf("/") + 1);
    }

    public void addModuleNameLabel() {
        addLabel("ts_quarkus_module_name", getModuleName());
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getQuarkusPluginVersion() {
        return quarkusPluginVersion;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public Map<String, String> getLabelsBucket() {
        return labelsBucket;
    }
}
