package io.quarkus.test.bootstrap;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.Command;

/**
 * Forward Docker ports from localhost to Docker host on Windows. This works around issue when
 * certificates are only generated for localhost.
 */
public final class LocalhostManagedResource implements ManagedResource {

    /**
     * Our Linux bare-metal instances use Docker on localhost.
     */
    private static final boolean FORWARD_PORT = OS.current() == OS.WINDOWS;
    private final ManagedResource delegate;

    public LocalhostManagedResource(ManagedResource delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public void stop() {
        if (FORWARD_PORT) {
            try {
                // stop port proxy
                new Command("netsh", "interface", "portproxy", "delete", "v4tov4",
                        "listenport=" + getExposedPort(), "listenaddress=127.0.0.1").runAndWait();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(
                        "Failed delete port proxy for Kafka container port " + getExposedPort(), e);
            }
        }
        delegate.stop();
    }

    @Override
    public void start() {
        delegate.start();
        if (FORWARD_PORT) {
            try {
                // forward localhost:somePort to dockerIp:somePort
                new Command("netsh", "interface", "portproxy", "add", "v4tov4", "listenport=" + getExposedPort(),
                        "listenaddress=127.0.0.1", "connectport=" + getExposedPort(),
                        "connectaddress=" + getDockerHost()).runAndWait();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(
                        "Failed to setup forwarding for Kafka container port " + getExposedPort(), e);
            }
        }
    }

    @Override
    public URILike getURI(Protocol protocol) {
        var uriLike = delegate.getURI(protocol);
        if (FORWARD_PORT) {
            // replace Docker IP with local host
            uriLike = new URILike(uriLike.getScheme(), "localhost", uriLike.getPort(), uriLike.getPath());
        }
        return uriLike;
    }

    private String getDockerHost() {
        return delegate.getURI(Protocol.NONE).getHost();
    }

    private int getExposedPort() {
        return delegate.getURI(Protocol.NONE).getPort();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public boolean isFailed() {
        return delegate.isFailed();
    }

    @Override
    public List<String> logs() {
        return delegate.logs();
    }

    @Override
    public void restart() {
        delegate.restart();
    }

    @Override
    public void validate() {
        delegate.validate();
    }

    @Override
    public void afterStart() {
        delegate.afterStart();
    }

    @Override
    public URILike createURI(String scheme, String host, int port) {
        return delegate.createURI(scheme, host, port);
    }
}
