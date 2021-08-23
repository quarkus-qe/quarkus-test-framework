package io.quarkus.test.bootstrap;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ServiceContext {

    private final Service owner;
    private final ScenarioContext scenarioContext;
    private final Path serviceFolder;
    private final Map<String, Object> store = new HashMap<>();

    protected ServiceContext(Service owner, ScenarioContext scenarioContext) {
        this.owner = owner;
        this.scenarioContext = scenarioContext;
        this.serviceFolder = Path.of("target", scenarioContext.getRunningTestClassName(), getName());
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

    public ExtensionContext getTestContext() {
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
}
