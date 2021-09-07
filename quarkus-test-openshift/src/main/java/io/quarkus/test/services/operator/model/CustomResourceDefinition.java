package io.quarkus.test.services.operator.model;

import io.fabric8.kubernetes.client.CustomResource;

public class CustomResourceDefinition {
    private final String name;
    private final String file;
    private final Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> type;

    public CustomResourceDefinition(String name, String file,
            Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> type) {
        this.name = name;
        this.file = file;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getFile() {
        return file;
    }

    public Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> getType() {
        return type;
    }
}
