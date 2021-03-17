package io.quarkus.test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ServiceContext {
    private final Service owner;
    private final ExtensionContext testContext;
    private final Path serviceFolder;
    private final Map<String, Object> store = new HashMap<>();

    protected ServiceContext(Service owner, ExtensionContext testContext) {
        this.owner = owner;
        this.testContext = testContext;
        this.serviceFolder = new File("target", owner.getName()).toPath();
    }

    public Service getOwner() {
        return owner;
    }

    public ExtensionContext getTestContext() {
        return testContext;
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
