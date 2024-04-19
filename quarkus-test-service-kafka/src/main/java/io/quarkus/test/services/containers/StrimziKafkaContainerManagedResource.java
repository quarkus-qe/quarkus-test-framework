package io.quarkus.test.services.containers;

import static me.escoffier.certs.Format.PKCS12;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.quarkus.test.services.containers.strimzi.ExtendedStrimziKafkaContainer;
import io.quarkus.test.utils.DockerUtils;
import me.escoffier.certs.CertificateGenerator;
import me.escoffier.certs.CertificateRequest;
import me.escoffier.certs.Pkcs12CertificateFiles;

public class StrimziKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    private static final String STRIMZI_SERVER_SSL = "strimzi-server-ssl";
    private static final String SSL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-ssl.properties";
    private static final String DEPRECATED_SSL_SERVER_KEYSTORE = "strimzi-default-server-ssl-keystore.p12";
    private static final String SSL_SERVER_KEYSTORE = STRIMZI_SERVER_SSL + "-keystore.p12";
    private static final String SSL_SERVER_TRUSTSTORE = STRIMZI_SERVER_SSL + "-truststore.p12";
    private static final String SASL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-sasl.properties";
    private static final String SASL_SSL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-sasl-ssl.properties";
    private static final String SASL_USERNAME_VALUE = "client";
    private static final String SASL_PASSWORD_VALUE = "client-secret";

    protected StrimziKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public String getDisplayName() {
        return KafkaVendor.STRIMZI.getImage() + getKafkaVersion();
    }

    @Override
    public URILike getURI(Protocol protocol) {
        var uri = super.getURI(protocol);
        if (model.getProtocol() == KafkaProtocol.SSL) {
            uri = uri.withScheme("SSL");
        } else if (model.getProtocol() == KafkaProtocol.SASL) {
            uri = uri.withScheme("SASL_PLAINTEXT");
        } else if (model.getProtocol() == KafkaProtocol.SASL_SSL) {
            uri = uri.withScheme("SASL_SSL");
        }
        return uri;
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        ExtendedStrimziKafkaContainer container = new ExtendedStrimziKafkaContainer(getKafkaImageName(), getKafkaVersion());
        if (StringUtils.isNotEmpty(getServerProperties())) {
            container.useCustomServerProperties();
        }
        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        return container;
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        GenericContainer<?> schemaRegistry = new GenericContainer<>(getKafkaRegistryImage());
        schemaRegistry.withExposedPorts(getKafkaRegistryPort());
        schemaRegistry.withEnv("APPLICATION_ID", "registry_id");
        schemaRegistry.withEnv("APPLICATION_SERVER", "localhost:9000");
        schemaRegistry.withEnv("KAFKA_BOOTSTRAP_SERVERS", "PLAINTEXT://localhost:" + getTargetPort());
        schemaRegistry.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        return schemaRegistry;
    }

    @Override
    protected String getServerProperties() {
        // Return user server properties if set
        if (StringUtils.isNotEmpty(model.getServerProperties())) {
            return model.getServerProperties();
        }

        // If not, if ssl enabled, overwrite server properties
        if (model.getProtocol() == KafkaProtocol.SSL) {
            return SSL_SERVER_PROPERTIES_DEFAULT;
        } else if (model.getProtocol() == KafkaProtocol.SASL) {
            return SASL_SERVER_PROPERTIES_DEFAULT;
        } else if (model.getProtocol() == KafkaProtocol.SASL_SSL) {
            return SASL_SSL_SERVER_PROPERTIES_DEFAULT;
        }

        return super.getServerProperties();
    }

    @Override
    protected String getResourceTargetName(String resource) {
        return DEPRECATED_SSL_SERVER_KEYSTORE.equals(resource) ? SSL_SERVER_KEYSTORE : resource;
    }

    @Override
    protected String[] getKafkaConfigResources() {
        List<String> effectiveUserKafkaConfigResources = new ArrayList<>();
        effectiveUserKafkaConfigResources.addAll(Arrays.asList(super.getKafkaConfigResources()));

        if (model.getProtocol() == KafkaProtocol.SSL || model.getProtocol() == KafkaProtocol.SASL_SSL) {
            final String trustStoreLocation;
            if (useDefaultServerProperties()) {
                if (useDefaultTrustStore()) {
                    // generate certs
                    final Path certsDir;
                    try {
                        certsDir = Files.createTempDirectory("certs");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    CertificateGenerator generator = new CertificateGenerator(certsDir, true);
                    CertificateRequest request = (new CertificateRequest()).withName(STRIMZI_SERVER_SSL)
                            .withClientCertificate(false).withFormat(PKCS12).withCN("localhost").withPassword("top-secret")
                            .withDuration(Duration.ofDays(2));
                    try {
                        var certFile = (Pkcs12CertificateFiles) generator.generate(request).get(0);
                        trustStoreLocation = certFile.trustStoreFile().toString();
                        effectiveUserKafkaConfigResources.add(trustStoreLocation);
                        effectiveUserKafkaConfigResources.add(certFile.keyStoreFile().toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // truststore in application resources dir
                    trustStoreLocation = SSL_SERVER_TRUSTSTORE;

                    // this we add for backwards compatibility with older tests
                    // TODO: remove deprecated keystore
                    effectiveUserKafkaConfigResources.add(DEPRECATED_SSL_SERVER_KEYSTORE);
                }
            } else {
                trustStoreLocation = "${custom-kafka-trust-store-location:}";
            }

            var configPropertyIterator = Map.of(
                    "kafka.ssl.enable", "true",
                    "kafka.ssl.truststore.location", trustStoreLocation,
                    "kafka.ssl.truststore.password", "top-secret",
                    "kafka.ssl.truststore.type", "PKCS12");
            if (model.getProtocol() == KafkaProtocol.SASL_SSL) {
                configPropertyIterator = new HashMap<>(configPropertyIterator);
                configPropertyIterator.put("kafka.security.protocol", "SASL_SSL");
                configPropertyIterator.put("kafka.sasl.mechanism", "PLAIN");
                configPropertyIterator.put("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required "
                                + "username=\"" + SASL_USERNAME_VALUE + "\" "
                                + "password=\"" + SASL_PASSWORD_VALUE + "\";");
                configPropertyIterator.put("ssl.endpoint.identification.algorithm", "https");
            }
            model.getContext().put(KafkaService.KAFKA_SSL_PROPERTIES, Map.copyOf(configPropertyIterator));
        }

        return effectiveUserKafkaConfigResources.toArray(new String[effectiveUserKafkaConfigResources.size()]);
    }

    private boolean useDefaultTrustStore() {
        return super.getKafkaConfigResources().length == 0;
    }

    private boolean useDefaultServerProperties() {
        return model.getServerProperties() == null || model.getServerProperties().isEmpty();
    }
}
