package io.quarkus.test.bootstrap;

import java.util.List;

public interface ManagedResource {

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
    String getHost(Protocol protocol);

    /**
     * Get the Port of the running resource.
     */
    int getPort(Protocol protocol);

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
}
