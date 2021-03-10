package io.quarkus.test;

import java.nio.file.Path;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ServiceContext {
	private final Service owner;
	private final Path serviceFolder;
	private final ExtensionContext testContext;

	protected ServiceContext(Service owner, Path serviceFolder, ExtensionContext testContext) {
		this.owner = owner;
		this.serviceFolder = serviceFolder;
		this.testContext = testContext;
	}

	public Service getOwner() {
		return owner;
	}

	public Path getServiceFolder() {
		return serviceFolder;
	}

	public ExtensionContext getTestContext() {
		return testContext;
	}
}
