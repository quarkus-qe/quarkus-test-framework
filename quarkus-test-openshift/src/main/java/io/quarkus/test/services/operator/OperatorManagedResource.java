package io.quarkus.test.services.operator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.OperatorService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.operator.model.CustomResourceDefinition;
import io.quarkus.test.utils.FileUtils;

public class OperatorManagedResource implements ManagedResource {

    private final OperatorManagedResourceBuilder model;
    private final OpenShiftClient client;
    private final List<CustomResourceDefinition> crdsToWatch = new ArrayList<>();

    private boolean running;

    public OperatorManagedResource(OperatorManagedResourceBuilder model) {
        this.model = model;
        this.client = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
    }

    @Override
    public String getDisplayName() {
        return model.getName();
    }

    @Override
    public void start() {
        if (!running) {
            installOperator();
            applyCRDs();

            running = true;
        }
    }

    @Override
    public void stop() {
        // Stop method in operators is not supported yet
    }

    @Override
    public boolean isRunning() {
        return running && customResourcesAreReady();
    }

    @Override
    public URILike getURI(Protocol protocol) {
        throw new UnsupportedOperationException("Operator does not expose services.");
    }

    @Override
    public List<String> logs() {
        // Logs in operators is not supported yet
        return Collections.emptyList();
    }

    private void applyCRDs() {
        if (model.getContext().getOwner() instanceof OperatorService<?> service) {
            for (var crd : service.getCrds()) {
                applyCRD(crd);
            }
        }
    }

    private void applyCRD(CustomResourceDefinition crd) {
        ServiceContext serviceContext = model.getContext();
        Path crdFileDefinition = serviceContext.getServiceFolder().resolve(crd.name());
        String content = FileUtils.loadFile(crd.file());
        FileUtils.copyContentTo(content, crdFileDefinition);

        client.apply(crdFileDefinition);
        crdsToWatch.add(crd);
    }

    private boolean customResourcesAreReady() {
        return crdsToWatch.stream().allMatch(crd -> client.isCustomResourceReady(crd.name(), crd.type()));
    }

    private void installOperator() {
        client.installOperator(model.getContext().getOwner(),
                model.getName(),
                model.getChannel(),
                model.getSource(),
                model.getSourceNamespace());
    }
}
