package io.quarkus.test.services.quarkus;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.apache.http.HttpStatus;

import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;

public abstract class OpenShiftQuarkusApplicationManagedResource<T extends QuarkusApplicationManagedResourceBuilder>
        extends QuarkusManagedResource {

    private static final int EXTERNAL_PORT = 80;

    protected final T model;
    protected final OpenShiftClient client;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;

    public OpenShiftQuarkusApplicationManagedResource(T model) {
        super(model.getContext());
        this.model = model;
        this.client = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
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

        loggingHandler = new OpenShiftLoggingHandler(model.getContext());
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
    public String getHost(Protocol protocol) {
        validateProtocol(protocol);
        return client.url(model.getContext().getOwner());
    }

    @Override
    public int getPort(Protocol protocol) {
        validateProtocol(protocol);
        return EXTERNAL_PORT;
    }

    @Override
    public boolean isRunning() {
        if (!running) {
            return false;
        }

        if (client.isServerlessService(model.getContext().getName())) {
            return routeIsReachable(Protocol.HTTP);
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
            fail("SSL is not supported for OpenShift tests yet");
        } else if (protocol == Protocol.GRPC) {
            fail("gRPC is not supported for OpenShift tests yet");
        }
    }

    private boolean routeIsReachable(Protocol protocol) {
        return given().baseUri(getHost(protocol)).basePath("/").port(getPort(protocol)).get()
                .getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE;
    }
}
