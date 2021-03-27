package io.quarkus.test.bootstrap;

@FunctionalInterface
public interface Action {
    void handle(Service service);
}
