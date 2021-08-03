package io.quarkus.test.bootstrap;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Operator;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftExtensionBootstrap implements ExtensionBootstrap {

    public static final String CLIENT = "openshift-client";

    private static final PropertyLookup PRINT_INFO_ON_ERROR = new PropertyLookup("ts.openshift.print.info.on.error",
            Boolean.TRUE.toString());
    private static final PropertyLookup DELETE_PROJECT_AFTER = new PropertyLookup("ts.openshift.delete.project.after.all",
            Boolean.TRUE.toString());

    private OpenShiftClient client;

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(OpenShiftScenario.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        client = OpenShiftClient.create();
        installOperators(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (DELETE_PROJECT_AFTER.getAsBoolean()) {
            client.deleteProject();
        }
    }

    @Override
    public void updateServiceContext(ServiceContext context) {
        context.put(CLIENT, client);
    }

    @Override
    public Optional<Object> getParameter(Class<?> clazz) {
        if (clazz == OpenShiftClient.class) {
            return Optional.of(client);
        }

        return Optional.empty();
    }

    @Override
    public void onError(ExtensionContext context, Throwable throwable) {
        if (PRINT_INFO_ON_ERROR.getAsBoolean()) {
            FileUtils.createDirectoryIfDoesNotExist(logsTestFolder(context));
            printStatus(context);
            printEvents(context);
            printPodLogs(context);
        }
    }

    private void printEvents(ExtensionContext context) {
        FileUtils.copyContentTo(client.getEvents(), logsTestFolder(context).resolve("events" + Log.LOG_SUFFIX));
    }

    private void printStatus(ExtensionContext context) {
        FileUtils.copyContentTo(client.getStatus(), logsTestFolder(context).resolve("status" + Log.LOG_SUFFIX));
    }

    private void printPodLogs(ExtensionContext context) {
        Map<String, String> logs = client.logs();
        for (Entry<String, String> podLog : logs.entrySet()) {
            FileUtils.copyContentTo(podLog.getValue(), logsTestFolder(context).resolve(podLog.getKey() + Log.LOG_SUFFIX));
        }
    }

    private Path logsTestFolder(ExtensionContext context) {
        return Log.LOG_OUTPUT_DIRECTORY.resolve(context.getRequiredTestClass().getSimpleName());
    }

    private void installOperators(ExtensionContext context) {
        OpenShiftScenario openShiftScenario = context.getRequiredTestClass().getAnnotation(OpenShiftScenario.class);
        for (Operator operator : openShiftScenario.operators()) {
            Service defaultService = new DefaultService();
            defaultService.register(operator.name(), context);
            client.installOperator(defaultService, operator.name(), operator.channel(), operator.source(),
                    operator.sourceNamespace());
        }
    }
}
