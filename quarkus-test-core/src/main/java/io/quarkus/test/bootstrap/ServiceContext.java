package io.quarkus.test.bootstrap;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ServiceContext {

    private final Service owner;
    private final ScenarioContext scenarioContext;
    private final Path serviceFolder;
    private final Map<String, Object> store = new HashMap<>();

    /**
     * Whatever we put into {@link Service#withProperty(String, String)} is stored inside static field instance.
     * Like 'static RestService app = new RestService()'.
     * If a couple of test classes has same superclass and the 'app' field is in that superclass, properties are shared.
     * That is if during the execution of the first test class you put there dynamically property "a",
     * the "a" property will be there when the next test class is executed.
     *
     * This field stores properties that has only a test class scope.
     */
    private final Map<String, String> configPropertiesWithTestScope = new HashMap<>();

    ServiceContext(Service owner, ScenarioContext scenarioContext) {
        this.owner = owner;
        this.scenarioContext = scenarioContext;
        if (getName().contains(":")) {
            this.serviceFolder = Path.of("target", scenarioContext.getRunningTestClassName(), getArtifactIdFromGav(getName()));
        } else {
            this.serviceFolder = Path.of("target", scenarioContext.getRunningTestClassName(), getName());
        }
    }

    public Service getOwner() {
        return owner;
    }

    public String getScenarioId() {
        return scenarioContext.getId();
    }

    public String getName() {
        return owner.getName();
    }

    public ScenarioContext getScenarioContext() {
        return scenarioContext;
    }

    public TestContext getTestContext() {
        return scenarioContext.getTestContext();
    }

    public Path getServiceFolder() {
        return serviceFolder;
    }

    public void put(String key, Object value) {
        store.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) store.get(key);
    }

    public ServiceContext withTestScopeConfigProperty(String key, String value) {
        configPropertiesWithTestScope.put(key, value);
        return this;
    }

    Map<String, String> getConfigPropertiesWithTestScope() {
        return configPropertiesWithTestScope;
    }

    private String getArtifactIdFromGav(String gav) {
        String artifactId = gav;
        int firstPos = gav.indexOf(":");
        int lastPos = gav.lastIndexOf(":");
        if (firstPos > 0) {
            if (lastPos == firstPos) { // g:a case
                artifactId = gav.substring(firstPos + 1);
            } else if (lastPos >= firstPos + 2) { // g:a:v case
                artifactId = gav.substring(firstPos + 1, lastPos);
            }
        }
        return artifactId;
    }
}
