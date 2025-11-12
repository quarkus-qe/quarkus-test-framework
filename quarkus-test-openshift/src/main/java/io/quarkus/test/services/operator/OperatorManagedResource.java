package io.quarkus.test.services.operator;

import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;

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
import io.quarkus.test.services.operator.model.CustomResourceInstance;
import io.quarkus.test.utils.FileUtils;

public class OperatorManagedResource implements ManagedResource {

    private final OperatorManagedResourceBuilder model;
    private final OpenShiftClient client;
    private final List<CustomResourceInstance> crsToWatch = new ArrayList<>();

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
            // operator might create a CustomResourceDefinitions during installation. Wait for those to be ready.
            waitForCRDs();
            applyCRs();

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

    private void applyCRs() {
        if (model.getContext().getOwner() instanceof OperatorService<?> service) {
            for (var cr : service.getCrs()) {
                applyCR(cr);
            }
        }
    }

    private void applyCR(CustomResourceInstance cr) {
        ServiceContext serviceContext = model.getContext();
        Path crFileDefinition = serviceContext.getServiceFolder().resolve(cr.name());
        String content = FileUtils.loadFile(cr.file());
        FileUtils.copyContentTo(content, crFileDefinition);

        client.apply(crFileDefinition);
        crsToWatch.add(cr);
    }

    private boolean customResourcesAreReady() {
        return crsToWatch.stream().allMatch(cr -> client.isCustomResourceReady(cr.name(), cr.type()));
    }

    private void waitForCRDs() {
        if (model.getContext().getOwner() instanceof OperatorService<?> service) {
            for (String crdName : service.getCrdNamesToWaitFor()) {
                untilIsTrue(() -> client.isCustomResourceDefinitionReady(crdName));
            }
        }
    }

    private void installOperator() {
        client.installOperator(model.getContext().getOwner(),
                model.getName(),
                model.getChannel(),
                model.getSource(),
                model.getSourceNamespace());
    }
}
