package io.quarkus.test.bootstrap.service.datagrid;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.service.OperatorOpenShiftInfinispan;

public class OperatorOpenShiftInfinispanResourceBuilder implements ManagedResourceBuilder {

    private ServiceContext context;

    private Path clientCertSecret;
    private Path clusterConfig;
    private Path clusterConfigMap;
    private Path connectSecret;
    private Path tlsSecret;
    private String clusterNameSpace;
    private String templateClusterNameSpace;
    private String templateTlsSecretName;
    private String templateConnectSecretName;

    protected Path getClientCertSecret() {
        return clientCertSecret;
    }

    protected Path getClusterConfig() {
        return clusterConfig;
    }

    protected Path getClusterConfigMap() {
        return clusterConfigMap;
    }

    protected Path getConnectSecret() {
        return connectSecret;
    }

    protected Path getTlsSecret() {
        return tlsSecret;
    }

    protected String getClusterNameSpace() {
        return clusterNameSpace;
    }

    protected String getTemplateClusterNameSpace() {
        return templateClusterNameSpace;
    }

    protected String getTemplateTlsSecretName() {
        return templateTlsSecretName;
    }

    protected String getTemplateConnectSecretName() {
        return templateConnectSecretName;
    }

    protected ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        OperatorOpenShiftInfinispan metadata = (OperatorOpenShiftInfinispan) annotation;
        clientCertSecret = Path.of(metadata.clientCertSecret());
        clusterConfig = Path.of(metadata.clusterConfig());
        clusterConfigMap = Path.of(metadata.clusterConfigMap());
        tlsSecret = Path.of(metadata.tlsSecret());
        connectSecret = Path.of(metadata.connectSecret());
        clusterNameSpace = metadata.clusterNameSpace();
        templateClusterNameSpace = metadata.templateClusterNameSpace();
        templateTlsSecretName = metadata.templateTlsSecretName();
        templateConnectSecretName = metadata.templateConnectSecretName();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        return new OperatorOpenShiftInfinispanManagedResource(this);
    }
}
