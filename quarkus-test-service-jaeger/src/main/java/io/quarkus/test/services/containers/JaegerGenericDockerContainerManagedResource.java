package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.JaegerService.JAEGER_TRACE_URL_PROPERTY;
import static io.quarkus.test.services.Certificate.Format.PEM;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.security.certificate.ClientCertificateRequest;
import io.quarkus.test.utils.DockerUtils;

public class JaegerGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {

    public static final String CERTIFICATE_CONTEXT_KEY = "io.quarkus.test.services.containers.jaeger.certificate";
    public static final String JAEGER_CLIENT_CERT_CN = "jaeger-client";
    private static final String COLLECTOR_OTLP_ENABLED = "COLLECTOR_OTLP_ENABLED";
    private final JaegerContainerManagedResourceBuilder model;

    protected JaegerGenericDockerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void start() {
        super.start();
        model.getContext().put(JAEGER_TRACE_URL_PROPERTY, getJaegerTraceUrl());
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> container = super.initContainer();
        container.addExposedPort(model.getTracePort());
        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));
        if (model.isTlsEnabled()) {
            var clientCertRequest = new ClientCertificateRequest[] {
                    new ClientCertificateRequest(JAEGER_CLIENT_CERT_CN, false) };
            var cert = Certificate.of("jaeger-cert", PEM, "jaeger-password", true, "jaeger-tls-config", clientCertRequest);
            model.getContext().put(CERTIFICATE_CONTEXT_KEY, cert);
            // I found CLI flags used below here: https://www.jaegertracing.io/docs/1.60/cli/
            container.withCreateContainerCmdModifier(cmd -> cmd
                    .withCmd("--collector.otlp.grpc.tls.enabled=true",
                            "--collector.otlp.grpc.tls.key=/test-tls/key/tls.key",
                            "--collector.otlp.grpc.tls.cert=/test-tls/cert/tls.cert",
                            "--collector.otlp.grpc.tls.client-ca=/test-tls/ca/ca.crt"));
            container.withCopyFileToContainer(MountableFile.forHostPath(cert.certPath()), "/test-tls/cert/tls.cert");
            container.withCopyFileToContainer(MountableFile.forHostPath(cert.keyPath()), "/test-tls/key/tls.key");
            container.withCopyFileToContainer(MountableFile.forHostPath(cert.truststorePath()), "/test-tls/ca/ca.crt");
        }

        if (model.shouldUseOtlpCollector()) {
            container.addEnv(COLLECTOR_OTLP_ENABLED, "true");
        }

        return container;
    }

    private String getJaegerTraceUrl() {
        return getURI(Protocol.HTTP)
                .withPort(getMappedPort(model.getTracePort()))
                .toString();
    }
}
