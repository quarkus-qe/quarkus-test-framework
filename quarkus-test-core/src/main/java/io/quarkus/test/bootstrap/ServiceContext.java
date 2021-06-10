package io.quarkus.test.bootstrap;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ServiceContext {
    private final Service owner;
    private final Optional<ExtensionContext> testContext;
    private final Path serviceFolder;
    private final Map<String, Object> store = new HashMap<>();

    protected ServiceContext(Service owner, ExtensionContext testContext) {
        this.owner = owner;
        this.testContext = Optional.ofNullable(testContext);
        this.serviceFolder = new File("target", getName()).toPath();
    }

    public Service getOwner() {
        return owner;
    }

    public String getName() {
        return owner.getName();
    }

    public ExtensionContext getTestContext() {
        if (testContext.isEmpty()) {
            throw new RuntimeException("Service has not been initialized with test context");
        }

        return testContext.get();
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
