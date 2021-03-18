package io.quarkus.test.logging;

import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.test.ServiceContext;
import io.quarkus.test.extension.OpenShiftExtensionBootstrap;
import io.quarkus.test.openshift.OpenShiftFacade;

public class OpenShiftLoggingHandler extends LoggingHandler {

    private final OpenShiftFacade facade;
    private final String serviceName;
    private Map<String, String> oldLogs;

    public OpenShiftLoggingHandler(ServiceContext context) {
        super(context);

        serviceName = context.getOwner().getName();
        facade = context.get(OpenShiftExtensionBootstrap.CLIENT);
    }

    @Override
    protected synchronized void handle() {
        Map<String, String> newLogs = facade.getLogs(serviceName);
        for (Entry<String, String> entry : newLogs.entrySet()) {
            onMapDifference(entry);
        }

        oldLogs = newLogs;
    }

    private void onMapDifference(Entry<String, String> entry) {
        String newPodLogs = formatPodLogs(entry.getKey(), entry.getValue());

        if (oldLogs != null && oldLogs.containsKey(entry.getKey())) {
            String oldPodLogs = formatPodLogs(entry.getKey(), oldLogs.get(entry.getKey()));

            onStringDifference(newPodLogs, oldPodLogs);
        } else {
            onLines(newPodLogs);
        }
    }

    private String formatPodLogs(String podName, String log) {
        return String.format("[%s] %s", podName, log);
    }

}
