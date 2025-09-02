package io.quarkus.test.bootstrap;

import static io.quarkus.test.services.containers.SqlServerManagedResourceBuilder.CERTIFICATE_PREFIX;

import java.util.Map;

import io.quarkus.test.security.certificate.Certificate.PemCertificate;
import io.quarkus.test.security.certificate.CertificateBuilder;

public class SqlServerService extends DatabaseService<SqlServerService> {

    private static final String USER = "sa";
    private static final String DEFAULT_PASSWORD = "My1337p@ssworD";
    private static final String DATABASE = "msdb";
    private static final String JDBC_NAME = "sqlserver";

    public SqlServerService() {
        super();
        withPassword(DEFAULT_PASSWORD);
        withProperty("ACCEPT_EULA", "Y");
        onPreStart(service -> service.withProperty("MSSQL_SA_PASSWORD", getPassword()));
    }

    @Override
    public String getUser() {
        return USER;
    }

    @Override
    public String getDatabase() {
        return DATABASE;
    }

    @Override
    public String getJdbcUrl() {
        var host = getURI();
        return "jdbc:" + getJdbcName() + "://" + host.getHost() + ":" + host.getPort() + ";databaseName=" + getDatabase();
    }

    /**
     * @see #getTlsProperties(String)
     */
    public Map<String, String> getTlsProperties() {
        return getTlsProperties(null);
    }

    /**
     * Additional JDBC extension properties configuring SQL Server driver to use encrypted communication.
     *
     * @param datasourceName datasource name
     * @return additional JDBC properties
     */
    public Map<String, String> getTlsProperties(String datasourceName) {
        CertificateBuilder certBuilder = getPropertyFromContext(CertificateBuilder.INSTANCE_KEY);
        if (certBuilder != null && certBuilder.findCertificateByPrefix(CERTIFICATE_PREFIX) instanceof PemCertificate pemCert) {
            final String additionalJdbcProperties;
            if (datasourceName != null && !datasourceName.isEmpty()) {
                additionalJdbcProperties = "quarkus.datasource.%s.jdbc.additional-jdbc-properties.".formatted(datasourceName);
            } else {
                additionalJdbcProperties = "quarkus.datasource.jdbc.additional-jdbc-properties.";
            }
            return Map.of(
                    additionalJdbcProperties + "trustStore", pemCert.truststorePath(),
                    additionalJdbcProperties + "trustStorePassword", pemCert.password(),
                    additionalJdbcProperties + "trustStoreType", "PKCS12",
                    additionalJdbcProperties + "trustServerCertificate", "false",
                    additionalJdbcProperties + "sslProtocol", "TLSv1.2",
                    additionalJdbcProperties + "authentication", "SqlPassword",
                    additionalJdbcProperties + "fips", "true",
                    additionalJdbcProperties + "encrypt", "true");
        }
        return Map.of();
    }

    @Override
    public SqlServerService withUser(String user) {
        throw new UnsupportedOperationException("You cannot configure a username for SQL Server");
    }

    @Override
    public SqlServerService withDatabase(String database) {
        throw new UnsupportedOperationException("You cannot configure a database for SQL Server");
    }

    @Override
    protected String getJdbcName() {
        return JDBC_NAME;
    }

}
