package io.quarkus.test;

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
	String getHost();

	/**
	 * Get the Port of the running resource.
	 */
	int getPort();

	/**
	 * @return if the resource is running.
	 */
	boolean isRunning();
}
