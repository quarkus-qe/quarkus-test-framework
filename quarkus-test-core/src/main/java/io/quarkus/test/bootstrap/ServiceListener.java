package io.quarkus.test.bootstrap;

public interface ServiceListener {

    default void onServiceError(ServiceContext service, Throwable throwable) {

    }

    default void onServiceStarted(ServiceContext service) {

    }

    default void onServiceStopped(ServiceContext service) {

    }
}
