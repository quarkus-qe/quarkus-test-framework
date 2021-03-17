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

import io.quarkus.test.extensions.ExtensionBootstrap;

public class QuarkusScenarioBootstrap implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private final ServiceLoader<AnnotationBinding> bindingsRegistry = ServiceLoader.load(AnnotationBinding.class);
    private final ServiceLoader<ExtensionBootstrap> extensionsRegistry = ServiceLoader.load(ExtensionBootstrap.class);

    private List<Service> services = new ArrayList<>();

    private List<ExtensionBootstrap> extensions;

    @Override
    public void beforeAll(ExtensionContext context) {
        extensions = initExtensions(context);
        extensions.forEach(ext -> ext.beforeAll(context));

        Class<?> currentClass = context.getRequiredTestClass();
        while (currentClass != Object.class) {
            initServicesFromClass(context, currentClass);
            currentClass = currentClass.getSuperclass();
        }

        services.forEach(Service::start);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try {
            services.forEach(Service::stop);
        } finally {
            extensions.forEach(ext -> ext.afterAll(context));
        }
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
        AnnotationBinding binding = bindingsRegistry.stream().map(Provider::get).filter(b -> b.isFor(field)).findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown annotation for service"));

        ManagedResourceBuilder resource = binding.createBuilder(field);

        field.setAccessible(true);
        Service service = (Service) field.get(null);
        ServiceContext serviceContext = new ServiceContext(service, context);
        extensions.forEach(ext -> ext.updateServiceContext(serviceContext));

        service.init(resource, serviceContext);
        services.add(service);
    }

    private List<ExtensionBootstrap> initExtensions(ExtensionContext context) {
        List<ExtensionBootstrap> list = new ArrayList<>();
        for (ExtensionBootstrap binding : extensionsRegistry) {
            if (binding.appliesFor(context)) {
                list.add(binding);
            }
        }

        return list;
    }

}
