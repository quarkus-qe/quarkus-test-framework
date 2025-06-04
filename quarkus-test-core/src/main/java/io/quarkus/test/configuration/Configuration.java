package io.quarkus.test.configuration;

import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public final class Configuration {

    private static final String GLOBAL_PROPERTIES = System.getProperty("ts.test.resources.file.location", "global.properties");
    private static final String TEST_PROPERTIES = "test.properties";
    private static final String PREFIX = "ts.";
    private static final String PREFIX_TEMPLATE = PREFIX + "%s.";
    private static final String GLOBAL_SCOPE = "global";

    private final EnumMap<Property, String> properties;

    private Configuration(EnumMap<Property, String> properties) {
        this.properties = properties;
    }

    public List<String> getAsList(Property property) {
        String value = get(property);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }

        return Stream.of(value.split(",")).collect(Collectors.toList());
    }

    public Duration getAsDuration(Property property, Duration defaultValue) {
        String value = get(property);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        if (Character.isDigit(value.charAt(0))) {
            value = "PT" + value;
        }

        return Duration.parse(value);
    }

    public Double getAsDouble(Property property, double defaultValue) {
        String value = get(property);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        return Double.parseDouble(value);
    }

    public int getAsInteger(Property property, int defaultValue) {
        String value = get(property);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    public String get(Property property) {
        return properties.get(property);
    }

    public String getOrDefault(Property property, String defaultValue) {
        return properties.getOrDefault(property, defaultValue);
    }

    public boolean isTrue(Property property) {
        return is(property, Boolean.TRUE.toString());
    }

    public boolean is(Property property, String expected) {
        return StringUtils.equalsIgnoreCase(properties.get(property), expected);
    }

    public static Configuration load() {
        EnumMap<Property, String> properties = new EnumMap<>(Property.class);
        // Lowest priority: properties from global.properties and scope `global`
        properties.putAll(loadPropertiesFrom(GLOBAL_PROPERTIES, GLOBAL_SCOPE));
        // Then, properties from system properties and scope `global`
        properties.putAll(loadPropertiesFromSystemProperties(GLOBAL_SCOPE));
        // Then, properties from test.properties and scope as global
        properties.putAll(loadPropertiesFrom(TEST_PROPERTIES, GLOBAL_SCOPE));

        return new Configuration(properties);
    }

    public static Configuration load(String... serviceNames) {
        Configuration configuration = load();
        for (String serviceName : serviceNames) {
            // Then, properties from test.properties and scope as service name
            configuration.properties.putAll(loadPropertiesFrom(TEST_PROPERTIES, serviceName));
            // Then, highest priority: properties from system properties and scope as service name
            configuration.properties.putAll(loadPropertiesFromSystemProperties(serviceName));
        }

        return configuration;
    }

    private static EnumMap<Property, String> loadPropertiesFromSystemProperties(String scope) {
        return loadPropertiesFrom(System.getProperties(), scope);
    }

    private static EnumMap<Property, String> loadPropertiesFrom(String propertiesFile, String scope) {
        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            Properties prop = new Properties();
            if (input != null) { // this can happen if file doesn't exist
                prop.load(input);
            }
            return loadPropertiesFrom(prop, scope);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static EnumMap<Property, String> loadPropertiesFrom(Properties prop, String scope) {
        EnumMap<Property, String> properties = new EnumMap<>(Property.class);

        String prefix = String.format(PREFIX_TEMPLATE, scope);
        for (Entry<Object, Object> entry : prop.entrySet()) {
            String key = (String) entry.getKey();
            if (StringUtils.startsWith(key, prefix)) {
                String property = key.replace(prefix, StringUtils.EMPTY);
                Property parsed = Property.getByName(property)
                        .orElseThrow(() -> new NoSuchElementException("Unknown property: " + property + " from " + key));
                properties.put(parsed, (String) entry.getValue());
            }
        }
        return properties;
    }

    public enum Property {
        RESOURCES_FILE_LOCATION("resources.file.location"),
        SERVICE_STARTUP_TIMEOUT("startup.timeout"),
        DELETE_FOLDER_ON_EXIT("delete.folder.on.exit"),
        SERVICE_STARTUP_CHECK_POLL_INTERVAL("startup.check-poll-interval"),
        TIMEOUT_FACTOR_PROPERTY("factor.timeout"),
        KUBERNETES_DEPLOYMENT_SERVICE_PROPERTY("kubernetes.service"),
        KUBERNETES_DEPLOYMENT_TEMPLATE_PROPERTY("kubernetes.template"),
        KUBERNETES_USE_INTERNAL_SERVICE_AS_URL_PROPERTY("kubernetes.use-internal-service-as-url"),
        KUBERNETES_DELETE_AFTERWARDS("kubernetes.delete.namespace.after.all"),
        KUBERNETES_EPHEMERAL_NAMESPACES("kubernetes.ephemeral.namespaces.enabled"),
        OPENSHIFT_DEPLOYMENT_SERVICE_PROPERTY("openshift.service"),
        OPENSHIFT_DEPLOYMENT_TEMPLATE_PROPERTY("openshift.template"),
        OPENSHIFT_USE_INTERNAL_SERVICE_AS_URL_PROPERTY("openshift.use-internal-service-as-url"),
        OPENSHIFT_DELETE_AFTERWARDS("openshift.delete.project.after.all"),
        OPENSHIFT_PRINT_ON_ERROR("openshift.print.info.on.error"),
        OPENSHIFT_EPHEMERAL_NAMESPACES("openshift.ephemeral.namespaces.enabled"),

        DELETE_IMAGE_ON_STOP_PROPERTY("container.delete.image.on.stop"),
        IMAGE_STREAM_TIMEOUT("imagestream.install.timeout"),
        OPERATOR_INSTALL_TIMEOUT("operator.install.timeout"),

        CREATE_SERVICE_BY_DEFAULT("generated-service.enabled"),
        PROPAGATE_PROPERTIES_STRATEGY("maven.propagate-properties-strategy"),
        PROPAGATE_PROPERTIES_STRATEGY_ALL_EXCLUSIONS("maven.propagate-properties-strategy.all.exclude"),
        PRIVILEGED_MODE("container.privileged-mode"),
        REUSABLE_MODE("container.reusable"),
        EXPECTED_OUTPUT("quarkus.expected.log"),
        PORT_RANGE_MIN("port.range.min"),
        PORT_RANGE_MAX("port.range.max"),
        PORT_RESOLUTION_STRATEGY("port.resolution.strategy"),

        METRICS_EXTENSION_ENABLED_PROPERTY("metrics.enabled"),
        METRICS_PUSH_AFTER_EACH_TEST("metrics.push-after-each-test"),
        METRICS_EXPORT_PROMETHEUS_PROPERTY("metrics.export.prometheus.endpoint"),
        JAEGER_HTTP_ENDPOINT_SYSTEM_PROPERTY("tracing.jaeger.endpoint"),

        LOG_ENABLE("log.enable"),
        LOG_LEVEL_NAME("log.level"),
        LOG_FORMAT("log.format"),
        LOG_FILE_OUTPUT("log.file.output"),
        LOG_NOCOLOR("log.nocolor"),
        CONTAINER_STARTUP_ATTEMPTS("container-startup-attempts"),
        JAEGER_TRACE_URL_PROPERTY("jaeger.trace.url"),
        GRAFANA_COLLECTOR_URL_PROPERTY("grafana.collector.url"),
        GRAFANA_REST_URL_PROPERTY("grafana.rest.url"),
        KAFKA_REGISTRY_URL_PROPERTY("kafka.registry.url"),
        KAFKA_SSL_PROPERTIES("kafka.ssl.properties"),
        SKIP_BEFORE_AND_AFTER("debug.skip-before-and-after-methods"),
        RUN_TESTS("debug.run-tests"),
        SUSPEND("debug.suspend"),
        DOCKER_DETECTION("docker-detection-enabled"),
        JAVA_ENABLE_PREVIEW("enable-java-preview"),
        IGNORE_KNOWN_ISSUE("global.ignore-known-issue"),
        CLI_CMD("quarkus.cli.cmd"),
        CONTAINER_REGISTRY_URL("container.registry-url"),
        CONTAINER_PREFIX("docker-container-prefix"),
        S2I_MAVEN_REMOTE_REPOSITORY("s2i.maven.remote.repository"),
        S2I_REPLACE_CA_CERTS("s2i.java.replace-ca-certs"),
        S2I_BASE_NATIVE_IMAGE("s2i.openshift.base-native-image"),
        CUSTOM_BUILD_REQUIRED("custom-build.required");

        private final String name;

        Property(String name) {
            this.name = name;
        }

        public String getName(@Nullable String scope) {
            String scopePart = "";
            if (scope != null) {
                scopePart = scope + ".";
            }
            return PREFIX + scopePart + name;
        }

        public String getName() {
            return name;
        }

        public String getGlobalScopeName() {
            return getName(GLOBAL_SCOPE);
        }

        public static Optional<Property> getByName(String requested) {
            return Arrays.stream(Property.values())
                    .filter(property -> property.name.equals(requested))
                    .findAny();
        }

        static Property byName(String requested) {
            return getByName(requested).orElseThrow(() -> new NoSuchElementException("Unknown property: " + requested));
        }

        public static boolean isKnownProperty(String toCheck) {
            return Arrays.stream(Property.values()).map(property -> property.name).anyMatch(name -> name.equals(toCheck));
        }
    }
}
