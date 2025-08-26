package io.quarkus.test.bootstrap;

import static io.quarkus.test.bootstrap.inject.KubectlClient.ENABLED_EPHEMERAL_NAMESPACES;
import static io.quarkus.test.configuration.Configuration.Property.KUBERNETES_DELETE_AFTERWARDS;

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

    private static final PropertyLookup DELETE_NAMESPACE_AFTER = new PropertyLookup(KUBERNETES_DELETE_AFTERWARDS.getName(),
            Boolean.TRUE.toString());

    private KubectlClient client;

    @Override
    public boolean appliesFor(ScenarioContext context) {
        boolean isValidConfig = context.isAnnotationPresent(KubernetesScenario.class);
        if (isValidConfig && !DELETE_NAMESPACE_AFTER.getAsBoolean() && ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean()) {
            Log.error("-Dts.kubernetes.delete.project.after.all=false is only supported with"
                    + " -Dts.kubernetes.ephemeral.namespaces.enabled=false");
            isValidConfig = false;
        }

        return isValidConfig;
    }

    @Override
    public void beforeAll(ScenarioContext context) {
        // if deleteNamespace and ephemeral namespaces are disabled then we are in debug mode. This mode is going to keep
        // all scenario resources in order to allow you to debug by yourself
        context.setDebug(!DELETE_NAMESPACE_AFTER.getAsBoolean() && !ENABLED_EPHEMERAL_NAMESPACES.getAsBoolean());
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
        Path testLogsPath = logsTestFolder(context);
        testLogsPath.toFile().mkdirs();
        for (Entry<String, String> podLog : logs.entrySet()) {
            FileUtils.copyContentTo(podLog.getValue(), logsTestFolder(context).resolve(podLog.getKey() + Log.LOG_SUFFIX));
        }
    }

    private Path logsTestFolder(ScenarioContext context) {
        return context.getLogFolder().resolve(context.getRunningTestClassName());
    }
}
