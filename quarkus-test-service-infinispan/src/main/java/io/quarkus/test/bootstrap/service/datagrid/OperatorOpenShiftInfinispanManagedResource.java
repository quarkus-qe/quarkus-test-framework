package io.quarkus.test.bootstrap.service.datagrid;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.Command;

/**
 * This setup infinispan/datagrid on openshift. There is need to have preinstalled specific operator on openshift instance.
 */
public class OperatorOpenShiftInfinispanManagedResource implements ManagedResource {

    private static final int PORT = 11222;

    private final OperatorOpenShiftInfinispanResourceBuilder model;
    private final OpenShiftClient openshiftClient;

    private final String clusterName;

    private Path pathOfClientCertSecret;
    private Path pathOfConnectSecret;
    private Path pathOfTlsSecret;
    private Path pathOfClusterConfig;
    private Path pathOfClusterConfigMap;
    private Path tmpDir;

    private boolean running = false;

    public OperatorOpenShiftInfinispanManagedResource(OperatorOpenShiftInfinispanResourceBuilder model) {
        this.model = model;
        this.openshiftClient = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
        clusterName = openshiftClient.project() + "-infinispan-cluster";
        try {
            tmpDir = Files.createDirectory(Path.of(System.getProperty("java.io.tmpdir"), clusterName));
            initResourceFiles();
        } catch (IOException e) {
            Assertions.fail("Error while copying the resource files. Caused by: " + e.getMessage());
        }
    }

    @Override
    public String getDisplayName() {
        return clusterName;
    }

    @Override
    public void start() {
        if (!running) {
            createInfinispanCluster();
            waitUntilInstanceReady();

            running = true;
        }
    }

    @Override
    public void stop() {
        if (running) {
            deleteInfinispanCluster();

            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public URILike getURI(Protocol protocol) {
        return new URILike(null, String.format("%s.%s.svc.cluster.local", clusterName, model.getClusterNameSpace()), PORT,
                null);
    }

    @Override
    public void restart() {
        scaleInfinispanTo(0);
        waitUntilInstanceShutdown();
        scaleInfinispanTo(1);
        waitUntilInstanceReady();
    }

    @Override
    public List<String> logs() {
        // Logs in Infinispan are not supported yet
        return Collections.emptyList();
    }

    /**
     * Create infinispan cluster in specific namespace and prepare secrets and configmap for app to connect to infinispan.
     */
    private void createInfinispanCluster() {
        String infinispanClusterNameSpace = model.getClusterNameSpace();
        prepareSecrets(infinispanClusterNameSpace);
        // create Infinispan cluster, need to replace cluster name to be unique and
        // secrets name to be same as created in `prepareSecrets`
        // This changed file is applied to namespace where infinispan operator is installed
        replaceInYml(pathOfClusterConfig, model.getTemplateClusterNameSpace(), clusterName);
        replaceInYml(pathOfClusterConfig, model.getTemplateConnectSecretName(),
                clusterName + "-" + model.getTemplateConnectSecretName());
        replaceInYml(pathOfClusterConfig, model.getTemplateTlsSecretName(),
                clusterName + "-" + model.getTemplateTlsSecretName());
        openshiftClient.applyInProject(pathOfClusterConfig, infinispanClusterNameSpace);

        // setup config map on app openshift project
        replaceInYml(pathOfClusterConfigMap, model.getTemplateClusterNameSpace(), clusterName);
        openshiftClient.apply(pathOfClusterConfigMap);
    }

    private void deleteInfinispanCluster() {
        String infinispanClusterNameSpace = model.getClusterNameSpace();
        openshiftClient.delete(pathOfClusterConfigMap);
        openshiftClient.deleteInProject(pathOfClusterConfig, infinispanClusterNameSpace);

        deleteSecrets(infinispanClusterNameSpace);
        running = false;
    }

    /**
     * Deploying the secrets to be used for app and infinispan cluster.
     * Modify the Infinispan cluster secrets to have unique name
     *
     * @param infinispanClusterNameSpace infinispan cluster namespace
     */
    private void prepareSecrets(String infinispanClusterNameSpace) {
        openshiftClient.apply(pathOfClientCertSecret);

        replaceInYml(pathOfConnectSecret, model.getTemplateConnectSecretName(),
                clusterName + "-" + model.getTemplateConnectSecretName());
        openshiftClient.applyInProject(pathOfConnectSecret, infinispanClusterNameSpace);

        replaceInYml(pathOfTlsSecret, model.getTemplateTlsSecretName(),
                clusterName + "-" + model.getTemplateTlsSecretName());
        openshiftClient.applyInProject(pathOfTlsSecret, infinispanClusterNameSpace);
    }

    private void deleteSecrets(String infinispanClusterNameSpace) {
        openshiftClient.delete(pathOfClientCertSecret);
        openshiftClient.deleteInProject(pathOfConnectSecret, infinispanClusterNameSpace);
        openshiftClient.deleteInProject(pathOfTlsSecret, infinispanClusterNameSpace);
    }

    private void waitUntilInstanceReady() {
        try {
            new Command("oc", "-n", model.getClusterNameSpace(), "wait", "--for", "condition=wellFormed", "--timeout=300s",
                    "infinispan/" + clusterName).runAndWait();
            running = true;
        } catch (Exception e) {
            Assertions.fail("Fail to check if Infinispan cluster is ready. Caused by: " + e.getMessage());
        }
    }

    private void waitUntilInstanceShutdown() {
        try {
            new Command("oc", "-n", model.getClusterNameSpace(), "wait", "--for", "condition=gracefulShutdown",
                    "--timeout=300s", "infinispan/" + clusterName).runAndWait();
            running = false;
        } catch (Exception e) {
            Assertions.fail("Fail to check if Infinispan cluster is shutdown. Caused by: " + e.getMessage());
        }
    }

    private void replaceInYml(Path yamlFile, String originalString, String newString) {
        try {
            Charset charset = StandardCharsets.UTF_8;
            String yamlContent = Files.readString(yamlFile, charset);
            yamlContent = yamlContent.replaceAll(originalString, newString);
            Files.writeString(yamlFile, yamlContent, charset);
        } catch (IOException ex) {
            Assertions.fail("Fail to adjust YAML file. Caused by: " + ex.getMessage());
        }
    }

    private void changeNumberOfReplicasInYml(Path yamlFile, int replicas) {
        try {
            Charset charset = StandardCharsets.UTF_8;
            String yamlContent = Files.readString(yamlFile, charset);
            yamlContent = yamlContent.replaceAll("replicas: \\d+", "replicas: " + replicas);
            Files.writeString(yamlFile, yamlContent, charset);
        } catch (IOException ex) {
            Assertions.fail("Fail to adjust YAML file. Caused by: " + ex.getMessage());
        }
    }

    /**
     * Scale the statefulset by updating and applying cluster config file.
     * Statefulset services needs to be rescaled by applying yaml config
     *
     * @param replicas number of replicas which should be Infinispan cluster scaled
     */
    private void scaleInfinispanTo(int replicas) {
        try {
            changeNumberOfReplicasInYml(pathOfClusterConfig, replicas);
            openshiftClient.applyInProject(pathOfClusterConfig, model.getClusterNameSpace());
        } catch (Exception e) {
            Assertions.fail("Fail to scale the Infinispan cluster. Caused by: " + e.getMessage());
        }
    }

    /**
     * Copy resources to tmp directory and set paths to these yml resources.
     * This is needed as we're modifying the content of yml and can't ensure that the original file
     * will be restored to default state.
     *
     * @throws IOException
     */
    private void initResourceFiles() throws IOException {
        pathOfClientCertSecret = addTmpDirToPathToResourceFile(model.getClientCertSecret());
        pathOfConnectSecret = addTmpDirToPathToResourceFile(model.getConnectSecret());
        pathOfTlsSecret = addTmpDirToPathToResourceFile(model.getTlsSecret());
        pathOfClusterConfig = addTmpDirToPathToResourceFile(model.getClusterConfig());
        pathOfClusterConfigMap = addTmpDirToPathToResourceFile(model.getClusterConfigMap());
        Files.copy(model.getClientCertSecret(), pathOfClientCertSecret);
        Files.copy(model.getConnectSecret(), pathOfConnectSecret);
        Files.copy(model.getTlsSecret(), pathOfTlsSecret);
        Files.copy(model.getClusterConfig(), pathOfClusterConfig);
        Files.copy(model.getClusterConfigMap(), pathOfClusterConfigMap);
    }

    /**
     * Create combined path of tmp directory path and name of resource file.
     *
     * @param resourcePath path to resource
     * @return new path to file in tmp directory
     */
    private Path addTmpDirToPathToResourceFile(Path resourcePath) {
        return Paths.get(tmpDir.toAbsolutePath().toString(), resourcePath.getFileName().toString());
    }
}
