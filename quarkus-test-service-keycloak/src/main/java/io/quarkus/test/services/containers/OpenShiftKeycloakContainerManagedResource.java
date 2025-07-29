package io.quarkus.test.services.containers;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;

public class OpenShiftKeycloakContainerManagedResource extends OpenShiftContainerManagedResource {

    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    KeycloakContainerManagedResourceBuilder model;

    protected OpenShiftKeycloakContainerManagedResource(KeycloakContainerManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    @Override
    public URILike getURI(Protocol protocol) {
        if (protocol == Protocol.HTTP) {
            return useInternalServiceAsUrl() ? createURI(protocol.getValue(), getInternalServiceName(), model.getPort())
                    : getClient().url(model.getContext().getOwner()).withPort(HTTP_PORT);
        } else {
            return useInternalServiceAsUrl() ? createURI(protocol.getValue(), getInternalServiceName(), model.getTlsPort())
                    : getClient().url(model.getContext().getOwner().getName() + "-secured").withPort(HTTPS_PORT);
        }
    }
}
