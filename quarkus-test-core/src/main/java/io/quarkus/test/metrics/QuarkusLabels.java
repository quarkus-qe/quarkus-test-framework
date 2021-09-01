package io.quarkus.test.metrics;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.utils.TestExecutionProperties;

public class QuarkusLabels {

    public static final String MODULE_STATUS = "module_status";
    public static final String MODULE_NAME = "module_name";
    public static final String SCENARIO_NAME = "scenario_name";

    private static final String MODULES_SUCCESS = "module_success";
    private static final String MODULES_FAIL = "module_fail";

    private static final String PLATFORM_LABEL_NAME = "execution_platform";
    private static final String PLUGIN_VERSION_LABEL_NAME = "execution_quarkus_version";
    private static final String SERVICE_NAME_LABEL_NAME = "execution_service_name";
    private static final String BUILD_NUMBER_LABEL_NAME = "execution_build_number";

    private final Map<String, String> labelsBucket = new HashMap<>();

    public QuarkusLabels() {
        labelsBucket.put(BUILD_NUMBER_LABEL_NAME, TestExecutionProperties.getBuildNumber());
        labelsBucket.put(PLUGIN_VERSION_LABEL_NAME, TestExecutionProperties.getVersionNumber());
        labelsBucket.put(SERVICE_NAME_LABEL_NAME, TestExecutionProperties.getServiceName());

        addPlatformLabel();
    }

    private void addPlatformLabel() {
        if (TestExecutionProperties.isOpenshiftPlatform()) {
            labelsBucket.put(PLATFORM_LABEL_NAME, "ocp");
        }

        if (TestExecutionProperties.isKubernetesPlatform()) {
            labelsBucket.put(PLATFORM_LABEL_NAME, "k8s");
        }

        if (TestExecutionProperties.isBareMetalPlatform()) {
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
        addLabel(MODULE_NAME, getModuleName());
    }

    public void addScenarioNameLabel(String scenarioName) {
        addLabel(SCENARIO_NAME, scenarioName);
    }

    public void markModuleAsSuccess() {
        getLabelsBucket().put(MODULE_STATUS, MODULES_SUCCESS);
    }

    public void markModuleAsFailed() {
        getLabelsBucket().put(MODULE_STATUS, MODULES_FAIL);
    }

    public Map<String, String> getLabelsBucket() {
        return labelsBucket;
    }
}
