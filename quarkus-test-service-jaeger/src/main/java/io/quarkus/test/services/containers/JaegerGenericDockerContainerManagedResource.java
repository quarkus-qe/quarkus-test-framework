package io.quarkus.test.services.containers;

import static io.quarkus.test.configuration.Configuration.Property.JAEGER_TRACE_URL_PROPERTY;
import static io.quarkus.test.services.Certificate.Format.PEM;

import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.security.certificate.ClientCertificateRequest;
import io.quarkus.test.utils.DockerUtils;

public class JaegerGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {

    public static final String CERTIFICATE_CONTEXT_KEY = "io.quarkus.test.services.containers.jaeger.certificate";
    public static final String JAEGER_CLIENT_CERT_CN = "jaeger-client";
    private final JaegerContainerManagedResourceBuilder model;

    protected JaegerGenericDockerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void start() {
        super.start();
        model.getContext().put(JAEGER_TRACE_URL_PROPERTY.getName(), getJaegerTraceUrl());
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> container = super.initContainer();
        container.addExposedPort(model.getTracePort());
        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        List<String> args = new ArrayList<>();
        args.add("--set");
        args.add("receivers.otlp.protocols.http.endpoint=0.0.0.0:" + model.getPort());
        args.add("--set");
        args.add("receivers.otlp.protocols.grpc.endpoint=0.0.0.0:4317");

        if (model.isTlsEnabled()) {
            var clientCertRequest = new ClientCertificateRequest[] {
                    new ClientCertificateRequest(JAEGER_CLIENT_CERT_CN, false) };
            var cert = Certificate.of("jaeger-cert", PEM, "jaeger-password", true, "jaeger-tls-config", clientCertRequest);
            model.getContext().put(CERTIFICATE_CONTEXT_KEY, cert);
            args.add("--set");
            args.add("receivers.otlp.protocols.http.tls.cert_file=/test-tls/cert/tls.cert");
            args.add("--set");
            args.add("receivers.otlp.protocols.http.tls.key_file=/test-tls/key/tls.key");
            args.add("--set");
            args.add("receivers.otlp.protocols.http.tls.client_ca_file=/test-tls/ca/ca.crt");
            container.withCopyFileToContainer(MountableFile.forHostPath(cert.certPath()), "/test-tls/cert/tls.cert");
            container.withCopyFileToContainer(MountableFile.forHostPath(cert.keyPath()), "/test-tls/key/tls.key");
            container.withCopyFileToContainer(MountableFile.forHostPath(cert.truststorePath()), "/test-tls/ca/ca.crt");
        }

        container.withCreateContainerCmdModifier(cmd -> cmd.withCmd(args.toArray(String[]::new)));

        return container;
    }

    private String getJaegerTraceUrl() {
        return getURI(Protocol.HTTP)
                .withPort(getMappedPort(model.getTracePort()))
                .toString();
    }
}
