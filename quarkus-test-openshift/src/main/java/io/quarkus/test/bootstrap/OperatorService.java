package io.quarkus.test.bootstrap;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.test.services.operator.model.CustomResourceDefinition;
import io.quarkus.test.services.operator.model.CustomResourceSpec;
import io.quarkus.test.services.operator.model.CustomResourceStatus;

public class OperatorService<T extends Service> extends BaseService<T> {

    private final List<CustomResourceDefinition> crds = new ArrayList<>();

    public List<CustomResourceDefinition> getCrds() {
        return crds;
    }

    public OperatorService<T> withCrd(String name, String crdFile,
            Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> type) {
        crds.add(new CustomResourceDefinition(name, crdFile, type));
        return this;
    }
}
