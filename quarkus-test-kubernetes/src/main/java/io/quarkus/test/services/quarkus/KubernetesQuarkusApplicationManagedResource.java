package io.quarkus.test.services.quarkus;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.apache.http.HttpStatus;

import io.quarkus.test.bootstrap.KubernetesExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.logging.KubernetesLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.services.URILike;

public abstract class KubernetesQuarkusApplicationManagedResource<T extends QuarkusApplicationManagedResourceBuilder>
        extends QuarkusManagedResource {

    protected final T model;
    protected final KubectlClient client;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;

    public KubernetesQuarkusApplicationManagedResource(T model) {
        super(model.getContext());
        this.model = model;
        this.client = model.getContext().get(KubernetesExtensionBootstrap.CLIENT);
    }

    protected abstract void doInit();

    protected abstract void doUpdate();

    @Override
    public void start() {
        if (running) {
            return;
        }

        if (!init) {
            doInit();
            init = true;
        } else {
            doUpdate();
        }

        client.scaleTo(model.getContext().getOwner(), 1);

        running = true;

        loggingHandler = new KubernetesLoggingHandler(model.getContext());
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        client.scaleTo(model.getContext().getOwner(), 0);
        running = false;
    }

    @Override
    public URILike getURI(Protocol protocol) {
        if (protocol == Protocol.HTTPS) {
            fail("SSL is not supported for Kubernetes tests yet");
        } else if (protocol == Protocol.GRPC) {
            fail("gRPC is not supported for Kubernetes tests yet");
        }
        return createURI(protocol.getValue(),
                client.host(model.getContext().getOwner()),
                client.port(model.getContext().getOwner()));
    }

    @Override
    public boolean isRunning() {
        if (!running) {
            return false;
        }

        return super.isRunning() && routeIsReachable(Protocol.HTTP);
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    @Override
    public void restart() {
        stop();
        if (model.containsBuildProperties()) {
            init = false;
            model.build();
        }

        start();
    }

    @Override
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    private void validateProtocol(Protocol protocol) {
        if (protocol == Protocol.HTTPS) {
            fail("SSL is not supported for Kubernetes tests yet");
        } else if (protocol == Protocol.GRPC) {
            fail("gRPC is not supported for Kubernetes tests yet");
        }
    }

    private boolean routeIsReachable(Protocol protocol) {
        var uri = getURI(protocol);

        return given().baseUri(uri.getRestAssuredStyleUri())
                .basePath("/")
                .port(uri.getPort())
                .get()
                .getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE;
    }
}
