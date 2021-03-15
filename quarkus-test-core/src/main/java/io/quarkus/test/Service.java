package io.quarkus.test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;
import io.restassured.specification.RequestSpecification;

public class Service {

	private final String serviceName;
	private final List<Action> onPreStartActions = new LinkedList<>();
	private final List<Action> onPostStartActions = new LinkedList<>();
    private final Map<String, String> properties = new HashMap<>();
    private final List<Runnable> futureProperties = new LinkedList<>();

	private ManagedResource managedResource;

	public Service(String name) {
		this.serviceName = name;

        onPreStart(s -> futureProperties.forEach(Runnable::run));
	}

	public String getName() {
		return serviceName;
	}

	public Service onPreStart(Action action) {
		onPreStartActions.add(action);
		return this;
	}

	public Service onPostStart(Action action) {
		onPostStartActions.add(action);
		return this;
	}

	/**
	 * The runtime configuration property to be used if the built artifact is
	 * configured to be run
	 */
	public Service withProperties(String propertiesFile) {
        properties.clear();
        properties.putAll(PropertiesUtils.toMap(propertiesFile));
		return this;
	}

	/**
	 * The runtime configuration property to be used if the built artifact is
	 * configured to be run
	 */
    public Service withProperty(String key, String value) {
        this.properties.put(key, value);
		return this;
	}

	/**
	 * The runtime configuration property to be used if the built artifact is
	 * configured to be run
	 */
    public Service withProperty(String key, Supplier<String> value) {
        futureProperties.add(() -> properties.put(key, value.get()));
		return this;
	}

	public String getHost() {
		return managedResource.getHost();
	}

	public Integer getPort() {
		return managedResource.getPort();
	}

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
	}

	/**
	 * Start the managed resource. If the managed resource is running, it does
	 * nothing.
	 *
	 * @throws RuntimeException when application errors at startup.
	 */
	public void start() {
		onPreStartActions.forEach(a -> a.handle(this));
		managedResource.start();
		waitUntilServiceIsStarted();
		onPostStartActions.forEach(a -> a.handle(this));
	}

	/**
	 * Stop the Quarkus application.
	 */
	public void stop() {
		if (managedResource != null) {
			managedResource.stop();
		}
	}

	public RequestSpecification restAssured() {
		return given().baseUri(managedResource.getHost()).basePath("/").port(managedResource.getPort());
	}

	protected void init(ManagedResourceBuilder managedResourceBuilder, ExtensionContext context) {
		Path serviceFolder = new File("target", serviceName).toPath();
        FileUtils.recreateDirectory(serviceFolder);

		managedResource = managedResourceBuilder.build(new ServiceContext(this, serviceFolder, context));
	}

	private void waitUntilServiceIsStarted() {
		await().atMost(5, TimeUnit.MINUTES).until(managedResource::isRunning);
	}
}
