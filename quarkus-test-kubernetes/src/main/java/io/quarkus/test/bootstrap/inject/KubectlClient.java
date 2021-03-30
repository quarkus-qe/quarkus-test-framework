package io.quarkus.test.bootstrap.inject;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;

public final class KubectlClient {

    private static final String KUBECTL = "kubectl";
    private static final int HTTP_PORT_DEFAULT = 80;

    private final String currentNamespace;
    private final DefaultOpenShiftClient masterClient;
    private final NamespacedOpenShiftClient client;

    private KubectlClient(String namespace) {
        currentNamespace = namespace;
        createProject(namespace);

        OpenShiftConfig config = new OpenShiftConfigBuilder().withTrustCerts(true).withNamespace(namespace).build();

        masterClient = new DefaultOpenShiftClient(config);
        client = masterClient.inNamespace(namespace);
    }

    /**
     * @return the current namespace
     */
    public String namespace() {
        return currentNamespace;
    }

    /**
     * Apply the file into Kubernetes.
     *
     * @param file
     */
    public void apply(Service service, Path file) {
        try {
            new Command(KUBECTL, "apply", "-f", file.toAbsolutePath().toString(), "-n", currentNamespace)
                    .runAndWait();
        } catch (Exception e) {
            fail("Failed to apply resource " + file.toAbsolutePath().toString() + " for " + service.getName() + ". Caused by "
                    + e.getMessage());
        }
    }

    /**
     * Update the file and then apply the file into Kubernetes.
     * A copy of the end template will be placed in the target location.
     *
     * @param file
     */
    public void applyServiceProperties(Service service, String file, UnaryOperator<String> update, Path target) {
        String content = FileUtils.loadFile(file);
        content = addPropertiesToDeployment(service.getProperties(), update.apply(content));
        apply(service, FileUtils.copyContentTo(content, target));
    }

    /**
     * Expose the service and port defined.
     *
     * @param service
     * @param port
     */
    public void expose(Service service, Integer port) {
        try {
            new Command(KUBECTL, "expose", "deployment", service.getName(), "--port=" + port, "--name=" + service.getName(),
                    "--type=LoadBalancer", "-n", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Service failed to be exposed. Caused by " + e.getMessage());
        }
    }

    /**
     * Scale the service to the replicas.
     *
     * @param service
     * @param replicas
     */
    public void scaleTo(Service service, int replicas) {
        try {
            new Command(KUBECTL, "scale", "deployment/" + service.getName(), "--replicas=" + replicas, "-n", currentNamespace)
                    .runAndWait();
        } catch (Exception e) {
            fail("Service failed to be scaled. Caused by " + e.getMessage());
        }
    }

    /**
     * Get all the logs for all the pods within one service.
     *
     * @param service
     * @return
     */
    public Map<String, String> logs(Service service) {
        Map<String, String> logs = new HashMap<>();
        for (Pod pod : client.pods().withLabel("deployment", service.getName()).list().getItems()) {
            if (isPodRunning(pod)) {
                String podName = pod.getMetadata().getName();
                logs.put(podName, client.pods().withName(podName).getLog());
            }
        }

        return logs;
    }

    /**
     * Resolve the url by the service.
     *
     * @param service
     * @return
     */
    public String url(Service service) {
        String serviceName = service.getName();
        io.fabric8.kubernetes.api.model.Service serviceModel = client.services().withName(serviceName).get();
        if (serviceModel == null
                || serviceModel.getStatus() == null
                || serviceModel.getStatus().getLoadBalancer() == null
                || serviceModel.getStatus().getLoadBalancer().getIngress() == null) {
            fail("Service " + serviceName + " not found");
        }

        Optional<String> ip = serviceModel.getStatus().getLoadBalancer().getIngress().stream()
                .map(LoadBalancerIngress::getIp)
                .filter(StringUtils::isNotEmpty)
                .findFirst();

        if (!ip.isPresent()) {
            fail("Service " + serviceName + " host not found");
        }

        return "http://" + ip.get();
    }

    /**
     * Resolve the port by the service.
     *
     * @param service
     * @return
     */
    public int port(Service service) {
        String serviceName = service.getName();
        io.fabric8.kubernetes.api.model.Service serviceModel = client.services().withName(serviceName).get();
        if (serviceModel == null || serviceModel.getSpec() == null || serviceModel.getSpec().getPorts() == null) {
            fail("Service " + serviceName + " not found");
        }

        return serviceModel.getSpec().getPorts().stream()
                .map(ServicePort::getPort)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(HTTP_PORT_DEFAULT);
    }

    /**
     * Delete the namespace and all the resources.
     */
    public void deleteNamespace() {
        try {
            new Command(KUBECTL, "delete", "namespace", currentNamespace).runAndWait();
        } catch (Exception e) {
            fail("Project failed to be deleted. Caused by " + e.getMessage());
        } finally {
            masterClient.close();
        }
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

    private List<HasMetadata> loadYaml(String template) {
        return client.load(new ByteArrayInputStream(template.getBytes())).get();
    }

    private String addPropertiesToDeployment(Map<String, String> properties, String template) {
        if (!properties.isEmpty()) {
            List<HasMetadata> objs = loadYaml(template);
            for (HasMetadata obj : objs) {
                if (obj instanceof Deployment) {
                    Deployment d = (Deployment) obj;
                    d.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                        properties.entrySet().forEach(
                                envVar -> container.getEnv().add(new EnvVar(envVar.getKey(), envVar.getValue(), null)));
                    });
                }
            }

            KubernetesList list = new KubernetesList();
            list.setItems(objs);
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Serialization.yamlMapper().writeValue(os, list);
                template = new String(os.toByteArray());
            } catch (IOException e) {
                fail("Failed adding properties into template. Caused by " + e.getMessage());
            }
        }

        return template;
    }

    public static KubectlClient create(String namespace) {
        return new KubectlClient(namespace);
    }

}
