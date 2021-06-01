package io.quarkus.test.services.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.OperatorService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.services.operator.model.CustomResourceDefinition;
import io.quarkus.test.utils.FileUtils;

public class OperatorManagedResource implements ManagedResource {

    private final OperatorManagedResourceBuilder model;
    private final OpenShiftClient client;

    private boolean running;
    private List<CustomResourceDefinition> crdsToWatch = new ArrayList<>();

    public OperatorManagedResource(OperatorManagedResourceBuilder model) {
        this.model = model;
        this.client = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
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
    public String getHost(Protocol protocol) {
        // Operator does not expose services.
        return null;
    }

    @Override
    public int getPort(Protocol protocol) {
        // Operator does not expose services.
        return 0;
    }

    @Override
    public List<String> logs() {
        // Logs in operators is not supported yet
        return Collections.emptyList();
    }

    private void applyCRDs() {
        if (model.getContext().getOwner() instanceof OperatorService) {
            OperatorService service = (OperatorService) model.getContext().getOwner();
            for (Object crd : service.getCrds()) {
                applyCRD((CustomResourceDefinition) crd);
            }
        }
    }

    private void applyCRD(CustomResourceDefinition crd) {
        ServiceContext serviceContext = model.getContext();
        String content = FileUtils.loadFile(crd.getFile());

        client.apply(serviceContext.getOwner(),
                FileUtils.copyContentTo(content, serviceContext.getServiceFolder().resolve(crd.getName())));

        if (crd.getType().isPresent()) {
            crdsToWatch.add(crd);
        }
    }

    private boolean customResourcesAreReady() {
        return crdsToWatch.stream().allMatch(crd -> client.isCustomResourceReady(crd.getName(), crd.getType().get()));
    }

    private void installOperator() {
        client.installOperator(model.getContext().getOwner(),
                model.getChannel(),
                model.getSource(),
                model.getSourceNamespace());
    }
}
