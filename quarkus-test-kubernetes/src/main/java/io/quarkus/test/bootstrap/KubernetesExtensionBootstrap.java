package io.quarkus.test.bootstrap;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.utils.FileUtils;

public class KubernetesExtensionBootstrap implements ExtensionBootstrap {

    public static final String CLIENT = "kubectl-client";
    private static final PropertyLookup DELETE_ON_FAIL = new PropertyLookup("ts.kubernetes.delete.namespace.on.failure",
            Boolean.TRUE.toString());

    private KubectlClient client;

    @Override
    public boolean appliesFor(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        client = KubectlClient.create();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (DELETE_ON_FAIL.getAsBoolean()) {
            client.deleteNamespace();
        }
    }

    @Override
    public void updateServiceContext(ServiceContext context) {
        context.put(CLIENT, client);
    }

    @Override
    public Optional<Object> getParameter(Class<?> clazz) {
        if (clazz == KubectlClient.class) {
            return Optional.of(client);
        }

        return Optional.empty();
    }

    @Override
    public void onError(ExtensionContext context, Throwable throwable) {
        Map<String, String> logs = client.logs();
        for (Entry<String, String> podLog : logs.entrySet()) {
            FileUtils.copyContentTo(podLog.getValue(), Paths.get(Log.LOG_OUTPUT_DIRECTORY, podLog.getKey() + Log.LOG_SUFFIX));
        }
    }
}
