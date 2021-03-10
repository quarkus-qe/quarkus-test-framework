package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class QuarkusScenarioBootstrap implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

	private final ServiceLoader<AnnotationBinding> bindings = ServiceLoader.load(AnnotationBinding.class);

	private List<Service> services = new ArrayList<>();

	@Override
	public void beforeAll(ExtensionContext context) {
		Class<?> currentClass = context.getRequiredTestClass();
		while (currentClass != Object.class) {
			initServicesFromClass(context, currentClass);
			currentClass = currentClass.getSuperclass();
		}

		services.forEach(Service::start);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		services.forEach(Service::stop);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		services.forEach(Service::start);
	}

	private void initServicesFromClass(ExtensionContext context, Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getType().isAssignableFrom(Service.class)) {
				try {
					initService(context, field);
				} catch (Exception e) {
					fail("Failed to start service: " + field + ". Caused by: " + e);
				}
			}
		}
	}

	private void initService(ExtensionContext context, Field field) throws Exception {
		AnnotationBinding binding = bindings.stream().map(Provider::get).filter(b -> b.isFor(field)).findFirst()
				.orElseThrow(() -> new RuntimeException("Unknown annotation for service"));

		ManagedResourceBuilder resource = binding.createBuilder(field);

		field.setAccessible(true);
		Service service = (Service) field.get(null);
		service.init(resource, context);
		services.add(service);

	}

}
