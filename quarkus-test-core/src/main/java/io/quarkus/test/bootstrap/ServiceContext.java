package io.quarkus.test.bootstrap;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ServiceContext {

    private final Service owner;
    private final ScenarioContext scenarioContext;
    private final Path serviceFolder;
    private final Map<String, Object> store = new HashMap<>();

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
