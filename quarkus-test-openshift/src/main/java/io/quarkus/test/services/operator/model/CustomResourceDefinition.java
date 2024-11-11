package io.quarkus.test.services.operator.model;

import io.fabric8.kubernetes.client.CustomResource;

public record CustomResourceDefinition(String name, String file,
        Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> type) {
}
