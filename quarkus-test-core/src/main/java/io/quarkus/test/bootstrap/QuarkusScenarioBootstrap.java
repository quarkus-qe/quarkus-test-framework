package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.logging.LogManager;

import javax.inject.Inject;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;

public class QuarkusScenarioBootstrap
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        ParameterResolver, LifecycleMethodExecutionExceptionHandler, TestWatcher {

    private final ServiceLoader<AnnotationBinding> bindingsRegistry = ServiceLoader.load(AnnotationBinding.class);
    private final ServiceLoader<ExtensionBootstrap> extensionsRegistry = ServiceLoader.load(ExtensionBootstrap.class);

    private List<Service> services = new ArrayList<>();

    private List<ExtensionBootstrap> extensions;

    public QuarkusScenarioBootstrap() {
        configureLogging();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        extensions = initExtensions(context);
        extensions.forEach(ext -> ext.beforeAll(context));

        Class<?> currentClass = context.getRequiredTestClass();
        while (currentClass != Object.class) {
            initResourcesFromClass(context, currentClass);
            currentClass = currentClass.getSuperclass();
        }

        services.forEach(service -> launchService(context, service));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        try {
            List<Service> servicesToStop = new ArrayList<>(services);
            Collections.reverse(servicesToStop);
            servicesToStop.forEach(Service::stop);
        } finally {
            extensions.forEach(ext -> ext.afterAll(context));
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        extensions.forEach(ext -> ext.beforeEach(context));
        services.forEach(Service::start);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensions.stream().anyMatch(ext -> ext.getParameter(parameterContext.getParameter().getType()).isPresent());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getParameter(parameterContext.getParameter().getName(), parameterContext.getParameter().getType());
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) {
        notifyExtensionsOnError(context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) {
        notifyExtensionsOnError(context, throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) {
        notifyExtensionsOnError(context, throwable);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        extensions.forEach(ext -> ext.onSuccess(context));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        extensions.forEach(ext -> ext.onError(context, cause));
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) {
        notifyExtensionsOnError(context, throwable);
    }

    private void launchService(ExtensionContext context, Service service) {
        try {
            extensions.forEach(ext -> ext.onServiceInitiate(context, service));
            service.start();
            extensions.forEach(ext -> ext.onServiceStarted(context, service));
        } catch (Error throwable) {
            extensions.forEach(ext -> ext.onServiceError(context, service, throwable));
            notifyExtensionsOnError(context, throwable);
            throw throwable;
        }
    }

    private void notifyExtensionsOnError(ExtensionContext context, Throwable throwable) {
        throwable.printStackTrace();
        extensions.forEach(ext -> ext.onError(context, throwable));
    }

    private void initResourcesFromClass(ExtensionContext context, Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            if (Service.class.isAssignableFrom(field.getType())) {
                initService(context, field);
            } else if (field.isAnnotationPresent(Inject.class)) {
                injectDependency(context, field);
            }
        }
    }

    private void injectDependency(ExtensionContext context, Field field) throws Exception {
        Object parameter = getParameter(field.getName(), field.getType());
        field.setAccessible(true);
        if (Modifier.isStatic(field.getModifiers())) {
            field.set(null, parameter);
        } else {
            fail("Fields can only be injected into static instances. Problematic field: " + field.getName());
        }
    }

    private void initService(ExtensionContext context, Field field) throws Exception {
        AnnotationBinding binding = bindingsRegistry.stream().map(Provider::get).filter(b -> b.isFor(field)).findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown annotation for service"));

        ManagedResourceBuilder resource = binding.createBuilder(field);

        field.setAccessible(true);
        Service service = (Service) field.get(null);
        service.validate(field);
        service.register(field.getName());
        ServiceContext serviceContext = new ServiceContext(service, context);
        extensions.forEach(ext -> ext.updateServiceContext(serviceContext));

        service.init(resource, serviceContext);
        services.add(service);
    }

    private Object getParameter(String name, Class<?> clazz) {
        Optional<Object> parameter = extensions.stream()
                .map(ext -> ext.getParameter(clazz))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (!parameter.isPresent()) {
            fail("Failed to inject: " + name);
        }

        return parameter.get();
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
        FileUtils.recreateDirectory(Paths.get(Log.LOG_OUTPUT_DIRECTORY));
        try (InputStream in = QuarkusScenarioBootstrap.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            // ignore
        }
    }
}
