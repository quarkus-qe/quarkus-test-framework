package io.quarkus.test.services.operator.model;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;

public class CustomResourceDefinition {
    private final String name;
    private final String file;
    private final Optional<Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>>> type;

    public CustomResourceDefinition(String name, String file) {
        this(name, file, null);
    }

    public CustomResourceDefinition(String name, String file,
            Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> type) {
        this.name = name;
        this.file = file;
        this.type = Optional.ofNullable(type);
    }

    public String getName() {
        return name;
    }

    public String getFile() {
        return file;
    }

    public Optional<Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>>> getType() {
        return type;
    }
}
