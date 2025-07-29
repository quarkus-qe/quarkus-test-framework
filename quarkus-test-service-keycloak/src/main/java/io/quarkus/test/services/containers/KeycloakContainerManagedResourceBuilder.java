package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.KeycloakService.KEYSTORE_PASSWORD;
import static io.quarkus.test.bootstrap.inject.OpenShiftClient.getOpenShiftUrl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.FixedPathContainerMountStrategy;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.utils.PropertiesUtils;

public class KeycloakContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    public static final String CERTIFICATE_CONTEXT_KEY = "io.quarkus.test.services.containers.keycloak.certificate";
    public static final String KEYCLOAK_PRODUCTION_MODE_KEY = "io.quarkus.test.services.keycloak.production.mode";

    private static final String KEYSTORE_PREFIX = "keycloak";
    private static final String MOUNTED_KEYSTORE_NAME = KEYSTORE_PREFIX + "-keystore";
    private static final String KEYSTORE_DEST_PATH = "/opt/keycloak/conf/";
    private static final String MOUNTED_KEYSTORE_PATH = KEYSTORE_DEST_PATH + MOUNTED_KEYSTORE_NAME;
    private static final String MOUNTED_KEY_PATH = KEYSTORE_DEST_PATH + "key/" + KEYSTORE_PREFIX + ".key";
    private static final String MOUNTED_CERT_PATH = KEYSTORE_DEST_PATH + "cert/" + KEYSTORE_PREFIX + ".crt";

    private final ServiceLoader<KeycloakContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(KeycloakContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private int restPort;
    private int tlsRestPort;
    private String[] command;
    private String expectedLog;
    private long memoryLimitMiB;
    private boolean runKeycloakInProdMode;
    private boolean sslEnabled;
    private Certificate.Format certificateFormat;

    @Override
    protected String getImage() {
        return image;
    }

    @Override
    protected Integer getPort() {
        return restPort;
    }

    @Override
    protected Integer getTlsPort() {
        return tlsRestPort;
    }

    @Override
    protected boolean isSslEnabled() {
        return sslEnabled;
    }

    @Override
    protected String[] getCommand() {
        return command;
    }

    @Override
    protected ServiceContext getContext() {
        return context;
    }

    @Override
    protected String getExpectedLog() {
        return expectedLog;
    }

    protected Long getMemoryLimitMiB() {
        return memoryLimitMiB;
    }

    protected boolean runKeycloakInProdMode() {
        return runKeycloakInProdMode;
    }

    protected Certificate.Format certificateFormat() {
        return certificateFormat;
    }

    @Override
    public void init(Annotation annotation) {
        KeycloakContainer metadata = (KeycloakContainer) annotation;
        this.image = PropertiesUtils.resolveProperty(metadata.image());
        this.restPort = metadata.port();
        this.tlsRestPort = metadata.tlsPort();
        this.expectedLog = PropertiesUtils.resolveProperty(metadata.expectedLog());
        this.command = metadata.command();
        this.memoryLimitMiB = metadata.memoryLimitMiB();
        this.runKeycloakInProdMode = metadata.runKeycloakInProdMode();
        this.certificateFormat = metadata.certificateFormat();
        // We want to set up sslEnable to same value as runKeycloakInProdMode as `isSslEnabled` is used to determine
        // if tls should be enabled
        this.sslEnabled = metadata.runKeycloakInProdMode();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;

        context.put(KEYCLOAK_PRODUCTION_MODE_KEY, runKeycloakInProdMode);

        if (runKeycloakInProdMode) {
            setUpProdKeycloak(context);
        }
        for (KeycloakContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new KeycloakGenericDockerContainerManagedResource(this);
    }

    private void setUpProdKeycloak(ServiceContext context) {
        String truststorePath = "";
        if (certificateFormat.equals(Certificate.Format.JKS) || certificateFormat.equals(Certificate.Format.PKCS12)) {
            var destinationStrategy = new FixedPathContainerMountStrategy(null,
                    MOUNTED_KEYSTORE_PATH + getSuffixOfStore(certificateFormat), null, null);

            var cert = io.quarkus.test.security.certificate.Certificate.of(KEYSTORE_PREFIX, certificateFormat,
                    KEYSTORE_PASSWORD, certTargetDir(), destinationStrategy, getSubjectAlternativeName());
            truststorePath = cert.truststorePath();

            cert.configProperties().forEach(context::withTestScopeConfigProperty);
            context.put(CERTIFICATE_CONTEXT_KEY, cert);

            enrichCommandByTlsCommands(false);
        } else if (certificateFormat.equals(Certificate.Format.PEM)) {
            var destinationStrategy = new FixedPathContainerMountStrategy(null, null,
                    MOUNTED_KEY_PATH, MOUNTED_CERT_PATH);

            var cert = io.quarkus.test.security.certificate.Certificate.of(KEYSTORE_PREFIX, certificateFormat,
                    KEYSTORE_PASSWORD, certTargetDir(), destinationStrategy, getSubjectAlternativeName());
            truststorePath = cert.truststorePath();

            cert.configProperties().forEach(context::withTestScopeConfigProperty);
            context.put(CERTIFICATE_CONTEXT_KEY, cert);

            enrichCommandByTlsCommands(true);
        } else {
            throw new IllegalArgumentException("Unsupported keystore format.");
        }

        try {
            // Need to copy the truststore to this location to enable some OpenShift Quarkus scenarios to work
            // Scenarios are UsingOpenShiftExtensionAndDockerBuildStrategy and UsingOpenShiftExtension
            Files.copy(Path.of(truststorePath),
                    Path.of("target", "classes", Path.of(truststorePath).getFileName().toString()).toAbsolutePath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy truststore. Full exception: " + e);
        }
    }

    private void enrichCommandByTlsCommands(boolean isPemFormatUsed) {
        ArrayList<String> list = Arrays.stream(command)
                .collect(Collectors.toCollection(ArrayList::new));
        if (isPemFormatUsed) {
            list.add("--https-certificate-file=" + MOUNTED_CERT_PATH);
            list.add("--https-certificate-key-file=" + MOUNTED_KEY_PATH);
        } else {
            list.add("--https-key-store-file=" + MOUNTED_KEYSTORE_PATH + getSuffixOfStore(certificateFormat));
            list.add("--https-key-store-password=" + KEYSTORE_PASSWORD);
        }
        command = list.toArray(new String[0]);
    }

    private List<String> getSubjectAlternativeName() {
        List<String> sans = new ArrayList<>();
        if (getOpenShiftUrl() == null) {
            sans.add("localhost");
        } else {
            // The SAN allow only have wildcard for lowest level (most left side) of subdomain
            sans.add(getOpenShiftUrl().getHost().replace("api.", "*.apps."));
        }
        return sans;
    }

    private static Path certTargetDir() {
        return Path.of("target");
    }

    private static String getSuffixOfStore(Certificate.Format format) {
        return switch (format) {
            case PKCS12 -> ".p12";
            case JKS -> ".jks";
            default -> throw new IllegalArgumentException(format + " is not supported to get suffix.");
        };
    }
}
