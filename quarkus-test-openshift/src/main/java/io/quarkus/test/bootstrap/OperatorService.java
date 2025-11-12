package io.quarkus.test.bootstrap;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.test.services.operator.model.CustomResourceInstance;
import io.quarkus.test.services.operator.model.CustomResourceSpec;
import io.quarkus.test.services.operator.model.CustomResourceStatus;

public class OperatorService<T extends Service> extends BaseService<T> {

    private final List<CustomResourceInstance> crs = new ArrayList<>();
    private final List<String> crdNamesToWaitFor = new ArrayList<>();

    public List<CustomResourceInstance> getCrs() {
        return crs;
    }

    public List<String> getCrdNamesToWaitFor() {
        return crdNamesToWaitFor;
    }

    public OperatorService<T> withCr(String name, String crFile,
            Class<? extends CustomResource<CustomResourceSpec, CustomResourceStatus>> type) {
        crs.add(new CustomResourceInstance(name, crFile, type));
        return this;
    }

    public OperatorService<T> waitForCRD(String crdName) {
        crdNamesToWaitFor.add(crdName);
        return this;
    }
}
