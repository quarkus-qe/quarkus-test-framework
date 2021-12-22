package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsNotNull;
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
    private static final int EXTERNAL_SSL_PORT = 443;

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
        return untilIsNotNull(() -> client.url(model.getContext().getOwner()),
                AwaitilitySettings.defaults().withService(getContext().getOwner()));
    }

    @Override
    public int getPort(Protocol protocol) {
        int port = client.isServerlessService(model.getContext().getName()) ? EXTERNAL_SSL_PORT : EXTERNAL_PORT;
        validateProtocol(protocol);
        return port;
    }

    @Override
    public boolean isRunning() {
        if (!running) {
            return false;
        }

        if (client.isServerlessService(model.getContext().getName())) {
            return routeIsReachable(Protocol.HTTPS);
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
        if (protocol == Protocol.HTTPS && !client.isServerlessService(model.getContext().getName())) {
            fail("SSL is not supported for OpenShift tests yet");
        } else if (protocol == Protocol.GRPC) {
            fail("gRPC is not supported for OpenShift tests yet");
        }
    }

    private boolean routeIsReachable(Protocol protocol) {
        return given().relaxedHTTPSValidation().baseUri(getHost(protocol)).basePath("/").port(getPort(protocol))
                .get().getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE;
    }
}
