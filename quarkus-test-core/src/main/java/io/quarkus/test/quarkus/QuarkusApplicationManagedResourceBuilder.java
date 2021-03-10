package io.quarkus.test.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.ManagedResource;
import io.quarkus.test.ManagedResourceBuilder;
import io.quarkus.test.NativeTest;
import io.quarkus.test.ServiceContext;
import io.quarkus.test.annotation.QuarkusApplication;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.utils.ClassPathUtils;

public class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

	private static final String QUARKUS_PACKAGE_TYPE_PROPERTY = "quarkus.package.type";
	private static final String NATIVE = "native";

	private Class<?>[] appClasses;

	private final ServiceLoader<QuarkusApplicationManagedResourceBinding> managedResourceBindings = ServiceLoader
			.load(QuarkusApplicationManagedResourceBinding.class);

	private ServiceContext context;
	private Path builtResultArtifact;

	protected Path getBuiltResultArtifact() {
		return builtResultArtifact;
	}

	protected ServiceContext getContext() {
		return context;
	}

	@Override
	public void init(Annotation annotation) {
		QuarkusApplication metadata = (QuarkusApplication) annotation;
		appClasses = metadata.classes();
		if (appClasses.length == 0) {
			appClasses = ClassPathUtils.findAllClassesFromSource();
		}
	}

	@Override
	public ManagedResource build(ServiceContext context) {
		this.context = context;
		buildArtifact();

		for (QuarkusApplicationManagedResourceBinding binding : managedResourceBindings) {
			if (binding.appliesFor(context)) {
				return binding.init(this, context);
			}
		}

		return new LocalhostQuarkusApplicationManagedResource(this);
	}

	private void buildArtifact() {
		try {
			Path appFolder = context.getServiceFolder();
			JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(appClasses);

			javaArchive.as(ExplodedExporter.class).exportExplodedInto(appFolder.toFile());

			Properties buildProperties = new Properties();
			if (isNativeTest()) {
				buildProperties.put(QUARKUS_PACKAGE_TYPE_PROPERTY, NATIVE);
			}

			Path testLocation = PathTestHelper.getTestClassesLocation(context.getTestContext().getRequiredTestClass());
			QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder().setApplicationRoot(appFolder)
					.setMode(QuarkusBootstrap.Mode.PROD).setLocalProjectDiscovery(true).addExcludedPath(testLocation)
					.setProjectRoot(testLocation).setBaseName(context.getOwner().getName())
					.setBuildSystemProperties(buildProperties).setTargetDirectory(appFolder);

			AugmentResult result;
			try (CuratedApplication curatedApplication = builder.build().bootstrap()) {
				AugmentAction action = curatedApplication.createAugmentor();

				result = action.createProductionApplication();
			}

			builtResultArtifact = Optional.ofNullable(result.getNativeResult())
					.orElseGet(() -> result.getJar().getPath());
		} catch (Exception ex) {
			fail("Failed to build Quarkus artifacts. Caused by " + ex);
		}
	}

	private boolean isNativeTest() {
		return context.getTestContext().getRequiredTestClass().isAnnotationPresent(NativeTest.class);
	}

}
