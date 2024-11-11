package io.quarkus.test.services.containers;

import static io.quarkus.test.services.Certificate.Format.PKCS12;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.quarkus.test.services.containers.strimzi.ExtendedStrimziKafkaContainer;
import io.quarkus.test.utils.DockerUtils;

public class StrimziKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    private static final String STRIMZI_SERVER_SSL = "strimzi-server-ssl";
    private static final String SSL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-ssl.properties";
    private static final String DEPRECATED_SSL_SERVER_KEYSTORE = "strimzi-default-server-ssl-keystore.p12";
    private static final String SSL_SERVER_KEYSTORE = STRIMZI_SERVER_SSL + "-keystore.p12";
    private static final String SSL_SERVER_TRUSTSTORE = STRIMZI_SERVER_SSL + "-truststore.p12";
    private static final String SASL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-sasl.properties";
    private static final String SASL_SSL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-sasl-ssl.properties";
    private static final String SASL_USERNAME_VALUE = "client";
    private static final String SASL_PASSWORD_VALUE = "client-secret12345678912345678912";

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
        ExtendedStrimziKafkaContainer container = new ExtendedStrimziKafkaContainer(getKafkaImageName(), getKafkaVersion())
                .enableKraftMode();
        if (StringUtils.isNotEmpty(getServerProperties())) {
            container.useCustomServerProperties();
        }
        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));
        if (model.getProtocol() == KafkaProtocol.SASL_SSL
                && (model.getServerProperties() == null || model.getServerProperties().isEmpty())) {
            /*
             * make sure that client is added before the start
             * https://docs.confluent.io/platform/current/kafka/authentication_sasl/authentication_sasl_scram.html#kraft-based-
             * clusters
             */
            container.configureScram(SASL_USERNAME_VALUE, SASL_PASSWORD_VALUE);
        }
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
                    var cert = Certificate.of(STRIMZI_SERVER_SSL, PKCS12, "top-secret");
                    trustStoreLocation = Objects.requireNonNull(cert.truststorePath());
                    effectiveUserKafkaConfigResources.add(trustStoreLocation);
                    effectiveUserKafkaConfigResources.add(Objects.requireNonNull(cert.keystorePath()));
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

            var configPropertyIterator = new HashMap<>();
            configPropertyIterator.put("kafka.ssl.enable", "true");
            configPropertyIterator.put("kafka.security.protocol", "SSL");
            if (model.getQuarkusTlsRegistryConfigName() == null) {
                configPropertyIterator.put("kafka.ssl.truststore.location", trustStoreLocation);
                configPropertyIterator.put("kafka.ssl.truststore.password", "top-secret");
                configPropertyIterator.put("kafka.ssl.truststore.type", "PKCS12");
            } else {
                final String tsConfigKeyPrefix = "quarkus.tls.%s.trust-store.p12."
                        .formatted(model.getQuarkusTlsRegistryConfigName());
                configPropertyIterator.put("kafka.tls-configuration-name", model.getQuarkusTlsRegistryConfigName());
                configPropertyIterator.put(tsConfigKeyPrefix + "path", trustStoreLocation);
                configPropertyIterator.put(tsConfigKeyPrefix + "password", "top-secret");
            }
            if (model.getProtocol() == KafkaProtocol.SASL_SSL) {
                configPropertyIterator = new HashMap<>(configPropertyIterator);
                configPropertyIterator.put("kafka.security.protocol", "SASL_SSL");
                configPropertyIterator.put("kafka.sasl.mechanism", "SCRAM-SHA-512");
                configPropertyIterator.put("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.scram.ScramLoginModule required "
                                + "username=\"" + SASL_USERNAME_VALUE + "\" "
                                + "password=\"" + SASL_PASSWORD_VALUE + "\";");
                configPropertyIterator.put("ssl.endpoint.identification.algorithm", "https");
            }
            model.getContext().put(KafkaService.KAFKA_SSL_PROPERTIES, Map.copyOf(configPropertyIterator));
        }

        return effectiveUserKafkaConfigResources.toArray(new String[0]);
    }

    private boolean useDefaultTrustStore() {
        return super.getKafkaConfigResources().length == 0;
    }

    private boolean useDefaultServerProperties() {
        return model.getServerProperties() == null || model.getServerProperties().isEmpty();
    }
}
