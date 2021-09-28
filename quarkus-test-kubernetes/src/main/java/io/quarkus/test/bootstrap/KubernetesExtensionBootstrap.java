package io.quarkus.test.bootstrap;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.scenarios.KubernetesScenario;
import io.quarkus.test.utils.FileUtils;

public class KubernetesExtensionBootstrap implements ExtensionBootstrap {
    public static final String CLIENT = "kubectl-client";

    private static final PropertyLookup DELETE_NAMESPACE_AFTER = new PropertyLookup("ts.kubernetes.delete.namespace.after.all",
            Boolean.TRUE.toString());

    private KubectlClient client;

    @Override
    public boolean appliesFor(ScenarioContext context) {
        return context.isAnnotationPresent(KubernetesScenario.class);
    }

    @Override
    public void beforeAll(ScenarioContext context) {
        client = KubectlClient.create(context.getId());
    }

    @Override
    public void afterAll(ScenarioContext context) {
        if (DELETE_NAMESPACE_AFTER.getAsBoolean()) {
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
    public void onError(ScenarioContext context, Throwable throwable) {
        Map<String, String> logs = client.logs();
        for (Entry<String, String> podLog : logs.entrySet()) {
            FileUtils.copyContentTo(podLog.getValue(), logsTestFolder(context).resolve(podLog.getKey() + Log.LOG_SUFFIX));
        }
    }

    private Path logsTestFolder(ScenarioContext context) {
        return context.getLogFolder().resolve(context.getRunningTestClassName());
    }
}
