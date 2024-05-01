package io.quarkus.test.services;

/**
 * Defines certificate requests for which this framework will generate a keystore and truststore.
 */
public @interface Certificate {

    enum Format {
        PEM,
        JKS,
        PKCS12
    }

    /**
     * Prefix keystore and truststore name with this attribute.
     */
    String prefix() default "quarkus-qe";

    /**
     * Secure file format.
     */
    Format format() default Format.PKCS12;

    /**
     * Keystore and truststore password.
     */
    String password() default "password";

    /**
     * Whether following configuration properties should be set for you:
     *
     * - `quarkus.http.ssl.certificate.key-store-file`
     * - `quarkus.http.ssl.certificate.key-store-file-type`
     * - `quarkus.http.ssl.certificate.key-store-password`
     *
     * You still can set and/or override these properties
     * with {@link io.quarkus.test.bootstrap.BaseService#withProperty(String, String)} service method.
     */
    boolean configureKeystore() default false;

    /**
     * Whether following configuration properties should be set for you:
     *
     * - `quarkus.http.ssl.certificate.trust-store-file`
     * - `quarkus.http.ssl.certificate.trust-store-file-type`
     * - `quarkus.http.ssl.certificate.trust-store-password`
     *
     * You still can set and/or override these properties
     * with {@link io.quarkus.test.bootstrap.BaseService#withProperty(String, String)} service method.
     */
    boolean configureTruststore() default false;

    /**
     * Whether following configuration properties should be set for you:
     *
     * - `quarkus.management.ssl.certificate.key-store-file`
     * - `quarkus.management.ssl.certificate.key-store-file-type`
     * - `quarkus.management.ssl.certificate.key-store-password`
     *
     * You still can set and/or override these properties
     * with {@link io.quarkus.test.bootstrap.BaseService#withProperty(String, String)} service method.
     */
    boolean configureKeystoreForManagementInterface() default false;

    /**
     * Specify client certificates that should be generated.
     * Generation of more than one client certificate is only implemented for {@link Format#PKCS12}.
     */
    ClientCertificate[] clientCertificates() default {};

    /**
     * Client certificates.
     */
    @interface ClientCertificate {
        /**
         * Common Name (CN) attribute within Distinguished Name (DN) of X.509 certificate.
         */
        String cnAttribute() default "localhost";

        /**
         * Whether generated client certificate should be added to the server truststore.
         */
        boolean unknownToServer() default false;
    }

}
