package io.quarkus.test.openshift;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.quarkus.test.utils.Command;

public final class OpenShiftFacade {

    private static final String OC = "oc";

    private final DefaultOpenShiftClient masterClient;
    private final NamespacedOpenShiftClient client;

    private OpenShiftFacade(String namespace) {
        createProject(namespace);

        OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(namespace).build();

        masterClient = new DefaultOpenShiftClient(config);
        client = masterClient.inNamespace(namespace);
    }

    public void createApplication(String name, String image) {
        try {
            new Command(OC, "new-app", image, "--name", name).runAndWait();
        } catch (Exception e) {
            fail("Application failed to be created. Caused by " + e.getMessage());
        }
    }

    public void exposeService(String serviceName, int port) {
        try {
            new Command(OC, "expose", "svc/" + serviceName, "--port=" + port).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be exposed. Caused by " + e.getMessage());
        }
    }

    public void setReplicaTo(String name, int replicas) {
        try {
            new Command(OC, "scale", "dc/" + name, "--replicas=" + replicas).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be exposed. Caused by " + e.getMessage());
        }
    }

    public Map<String, String> getLogs(String name) {
        Map<String, String> logs = new HashMap<>();
        for (Pod pod : client.pods().withLabel("deploymentconfig", name).list().getItems()) {
            if (isPodRunning(pod)) {
                String podName = pod.getMetadata().getName();
                logs.put(podName, client.pods().withName(podName).getLog());
            }
        }

        return logs;
    }

    public String getUrlFromRoute(String routeName) {
        Route route = client.routes().withName(routeName).get();
        if (route == null || route.getSpec() == null) {
            fail("Route " + routeName + " not found");
        }

        final String protocol = route.getSpec().getTls() == null ? "http" : "https";
        final String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
        return String.format("%s://%s%s", protocol, route.getSpec().getHost(), path);
    }

    private boolean isPodRunning(Pod pod) {
        return pod.getStatus().getPhase().equals("Running");
    }

    private void createProject(String projectName) {
        try {
            new Command(OC, "new-project", projectName).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be created. Caused by " + e.getMessage());
        }
    }

    public void deleteProject() {
        try {
            new Command(OC, "delete", "project", client.getNamespace()).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            masterClient.close();
        }
    }

    public static final OpenShiftFacade create(String namespace) {
        return new OpenShiftFacade(namespace);
    }

}
