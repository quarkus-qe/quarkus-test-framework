package io.quarkus.test.services.containers;

import static io.quarkus.test.services.Certificate.Format.PEM;
import static io.quarkus.test.utils.PropertiesUtils.DESTINATION_TO_FILENAME_SEPARATOR;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.security.certificate.FixedPathContainerMountStrategy;
import io.quarkus.test.services.SqlServerContainer;

public final class SqlServerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    public static final String CERTIFICATE_PREFIX = "mssql";
    private boolean tlsEnabled = false;

    @Override
    public void init(Annotation annotation) {
        if (annotation instanceof SqlServerContainer sqlServerContainer) {
            tlsEnabled = sqlServerContainer.tlsEnabled();
            init(sqlServerContainer.image(), new String[0], sqlServerContainer.expectedLog(), sqlServerContainer.port(),
                    tlsEnabled);
        } else {
            throw new IllegalStateException("Expected annotation SqlServerContainer, but got: " + annotation);
        }
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        if (!tlsEnabled) {
            return super.build(context);
        }

        var destStrategy = new FixedPathContainerMountStrategy("/etc/ssl/ca-crt/mssql-ca.crt", null,
                "/etc/ssl/private/mssql.key", "/etc/ssl/certs/mssql.crt");
        var cert = Certificate.of(CERTIFICATE_PREFIX, PEM, "password", certTargetDir(), destStrategy, true);

        cert.configProperties().forEach(context::withTestScopeConfigProperty);
        context.withTestScopeConfigProperty("mssql-config", createConfigFileProperty());
        context.put(CertificateBuilder.INSTANCE_KEY, CertificateBuilder.of(cert));

        return super.build(context);
    }

    private static String createConfigFileProperty() {
        return RESOURCE_WITH_DESTINATION_PREFIX + "/var/opt/mssql/" + DESTINATION_TO_FILENAME_SEPARATOR + "mssql.conf";
    }

    private static Path certTargetDir() {
        // when mounting to Docker we are not searching for files recursively, so we put certs directly to the target dir
        return Path.of("target");
    }
}
