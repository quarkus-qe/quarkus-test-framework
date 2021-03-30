package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.logging.LogManager;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class QuarkusScenarioBootstrap implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, ParameterResolver {

    private final ServiceLoader<AnnotationBinding> bindingsRegistry = ServiceLoader.load(AnnotationBinding.class);
    private final ServiceLoader<ExtensionBootstrap> extensionsRegistry = ServiceLoader.load(ExtensionBootstrap.class);

    private List<Service> services = new ArrayList<>();

    private List<ExtensionBootstrap> extensions;

    public QuarkusScenarioBootstrap() {
        configureLogging();
    }

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

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensions.stream().anyMatch(ext -> ext.supportsParameter(parameterContext, extensionContext));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Optional<Object> parameter = extensions.stream()
                .filter(ext -> ext.supportsParameter(parameterContext, extensionContext))
                .map(ext -> ext.resolveParameter(parameterContext, extensionContext))
                .filter(Objects::nonNull)
                .findFirst();

        if (!parameter.isPresent()) {
            fail("Failed to inject parameter: " + parameterContext.getParameter().getName());
        }

        return parameter.get();
    }

    private void initServicesFromClass(ExtensionContext context, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Service.class.isAssignableFrom(field.getType())) {
                try {
                    initService(context, field);
                } catch (Exception e) {
                    e.printStackTrace();
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
        service.register(field.getName());
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

    private void configureLogging() {
        Locale.setDefault(new Locale("en", "EN"));
        try (InputStream in = QuarkusScenarioBootstrap.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            // ignore
        }
    }
}
