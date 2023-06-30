package io.quarkus.test.bootstrap;

import java.util.List;

import io.quarkus.test.services.URILike;

public interface ManagedResource {

    /**
     * @return name of the running resource.
     */
    String getDisplayName();

    /**
     * Start the resource. If the resource is already started, it will do nothing.
     *
     * @throws RuntimeException when application errors at startup.
     */
    void start();

    /**
     * Stop the resource.
     */
    void stop();

    /**
     * Get the Host of the running resource.
     */
    URILike getURI(Protocol protocol);

    /**
     * @return if the resource is running.
     */
    boolean isRunning();

    /**
     * @return if the resource has failed.
     */
    default boolean isFailed() {
        return false;
    }

    /**
     * @return the list of logs.
     */
    List<String> logs();

    /**
     * Restart of the managed resource.
     */
    default void restart() {
        stop();
        start();
    }

    default void validate() {
    }

    /*
     * An action, which should be executed as soon as the resource is started.
     */
    default void afterStart() {

    }

    default URILike createURI(String scheme, String host, int port) {
        return new URILike(scheme, host, port, null);
    }
}
