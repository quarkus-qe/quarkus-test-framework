/*  File: KubernetesKeycloakContainerManagedResource.java
    Author: Georgii Troitskii (xtroit00)
    Date: 9.5.2024
*/

package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.List;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.URILike;

public class KubernetesKeycloakContainerManagedResource extends KubernetesContainerManagedResource {

    protected static final String DEPLOYMENT_TEMPLATE_PROPERTY_HTTPS_DEFAULT = "/kubernetes-deployment-https-template.yml";

    protected KubernetesKeycloakContainerManagedResource(KeycloakContainerManagedResourceBuilder model) {
        super(adaptModel(model));
    }

    @Override
    public URILike getURI(Protocol protocol) {
        if (protocol == Protocol.HTTPS && !useInternalServiceAsUrl()) {
            return createURI(Protocol.HTTPS.getValue(), getClient().host(),
                    getClient().port(getModel().getContext().getOwner()));
        }
        return super.getURI(protocol);
    }

    @Override
    protected String getTemplate() {
        if (getModel() instanceof KeycloakContainerManagedResourceBuilder keycloakModel
                && !keycloakModel.runKeycloakInProdMode()) {
            return super.getTemplate();
        }
        return DEPLOYMENT_TEMPLATE_PROPERTY_HTTPS_DEFAULT;
    }

    private static ContainerManagedResourceBuilder adaptModel(KeycloakContainerManagedResourceBuilder model) {
        if (!model.runKeycloakInProdMode()) {
            return model;
        }
        return new HttpsContainerManagedResourceBuilder(model);
    }

    private static final class HttpsContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

        private final KeycloakContainerManagedResourceBuilder delegate;

        private HttpsContainerManagedResourceBuilder(KeycloakContainerManagedResourceBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        protected Integer getPort() {
            // we only support one port, because Keycloak only listen on one port
            return delegate.getTlsPort();
        }

        @Override
        protected String getImage() {
            return delegate.getImage();
        }

        @Override
        protected String getExpectedLog() {
            return delegate.getExpectedLog();
        }

        @Override
        protected String[] getCommand() {
            return delegate.getCommand();
        }

        @Override
        protected Integer getTlsPort() {
            return delegate.getTlsPort();
        }

        @Override
        protected boolean isSslEnabled() {
            return delegate.isSslEnabled();
        }

        @Override
        protected ServiceContext getContext() {
            return delegate.getContext();
        }

        @Override
        public void init(Annotation annotation) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void init(String image, String[] command, String expectedLog, int port, boolean portDockerHostToLocalhost) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void init(String image, String[] command, String expectedLog, int port, int tlsPort, boolean sslEnabled,
                boolean portDockerHostToLocalhost) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManagedResource build(ServiceContext context) {
            return delegate.build(context);
        }

        @Override
        public List<MountConfig> getMounts() {
            return delegate.getMounts();
        }
    }
}
