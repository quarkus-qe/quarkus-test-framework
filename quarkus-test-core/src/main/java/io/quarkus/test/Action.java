package io.quarkus.test;

@FunctionalInterface
public interface Action {
	void handle(Service service);
}
