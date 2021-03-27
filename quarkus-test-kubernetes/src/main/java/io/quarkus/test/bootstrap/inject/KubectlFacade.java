package io.quarkus.test.bootstrap.inject;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.quarkus.test.utils.Command;

public final class KubectlFacade {

    private static final String KUBECTL = "kubectl";
    private static final int HTTP_PORT_DEFAULT = 80;

    private final String currentNamespace;
    private final DefaultOpenShiftClient masterClient;
    private final NamespacedOpenShiftClient client;

    private KubectlFacade(String namespace) {
        currentNamespace = namespace;
        createProject(namespace);

        OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(namespace).build();

        masterClient = new DefaultOpenShiftClient(config);
        client = masterClient.inNamespace(namespace);
    }

    public String getNamespace() {
        return currentNamespace;
    }

    public List<HasMetadata> loadYaml(String template) {
        return client.load(new ByteArrayInputStream(template.getBytes())).get();
    }

    public void apply(Path file) {
        try {
            new Command(KUBECTL, "apply", "-f", file.toAbsolutePath().toString(), "-n", currentNamespace)
                    .runAndWait();
        } catch (Exception e) {
            fail("Failed to apply resource " + file.toAbsolutePath().toString() + " . Caused by " + e.getMessage());
        }
    }

    public void createApplication(String name, String image) {
        try {
            new Command(KUBECTL, "create", "deployment", name, "--image=" + image, "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Application failed to be created. Caused by " + e.getMessage());
        }
    }

    public void exposeService(String name, Integer port) {
        try {
            new Command(KUBECTL, "expose", "deployment", name, "--port=" + port, "--name=" + name, "--type=LoadBalancer",
                    "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be created. Caused by " + e.getMessage());
        }
    }

    public void setReplicaTo(String name, int replicas) {
        try {
            new Command(KUBECTL, "scale", "deployment/" + name, "--replicas=" + replicas, "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be exposed. Caused by " + e.getMessage());
        }
    }

    public Map<String, String> getLogs(String serviceName) {
        Map<String, String> logs = new HashMap<>();
        for (Pod pod : client.pods().withLabel("app", serviceName).list().getItems()) {
            if (isPodRunning(pod)) {
                String podName = pod.getMetadata().getName();
                logs.put(podName, client.pods().withName(podName).getLog());
            }
        }

        return logs;
    }

    public boolean hasImageStreamTags(ImageStream is) {
        return !masterClient.imageStreams().withName(is.getMetadata().getName()).get().getSpec().getTags().isEmpty();
    }

    public String getUrlByService(String serviceName) {
        Service service = client.services().withName(serviceName).get();
        if (service == null
                || service.getStatus() == null
                || service.getStatus().getLoadBalancer() == null
                || service.getStatus().getLoadBalancer().getIngress() == null) {
            fail("Service " + serviceName + " not found");
        }

        Optional<String> ip = service.getStatus().getLoadBalancer().getIngress().stream()
                .map(LoadBalancerIngress::getIp)
                .filter(StringUtils::isNotEmpty)
                .findFirst();

        if (!ip.isPresent()) {
            fail("Service " + serviceName + " host not found");
        }

        return "http://" + ip.get();
    }

    public int getPortByService(String serviceName) {
        Service service = client.services().withName(serviceName).get();
        if (service == null || service.getSpec() == null || service.getSpec().getPorts() == null) {
            fail("Service " + serviceName + " not found");
        }

        return service.getSpec().getPorts().stream()
                .map(ServicePort::getPort)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(HTTP_PORT_DEFAULT);
    }

    private boolean isPodRunning(Pod pod) {
        return pod.getStatus().getPhase().equals("Running");
    }

    private void createProject(String projectName) {
        try {
            new Command(KUBECTL, "create", "namespace", projectName).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be created. Caused by " + e.getMessage());
        }
    }

    public void deleteProject() {
        try {
            new Command(KUBECTL, "delete", "namespace", client.getNamespace()).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            masterClient.close();
        }
    }

    public static KubectlFacade create(String namespace) {
        return new KubectlFacade(namespace);
    }

}
