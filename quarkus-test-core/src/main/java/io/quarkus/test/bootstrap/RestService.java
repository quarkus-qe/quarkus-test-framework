package io.quarkus.test.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.TestExecutionProperties;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class RestService extends BaseService<RestService> {

    private static final int BUFFER_SIZE = 1024;
    private static final String BASE_PATH = "/";

    private WebClient webClient;

    public RequestSpecification given() {
        var host = getURI(Protocol.HTTP);
        return RestAssured.given()
                .baseUri(host.getRestAssuredStyleUri())
                .basePath(BASE_PATH)
                .port(host.getPort());
    }

    public RequestSpecification https() {
        Protocol protocol = Protocol.HTTPS;
        var host = getURI(protocol);
        return RestAssured.given()
                .baseUri(host.getRestAssuredStyleUri())
                .basePath(BASE_PATH)
                .port(host.getPort());
    }

    public RequestSpecification management() {
        var host = getURI(Protocol.MANAGEMENT);
        if (host.getScheme().equals(Protocol.MANAGEMENT.getValue())) {
            throw new IllegalArgumentException("Can not find URL to the management interface");
        }
        return RestAssured.given()
                .baseUri(host.getRestAssuredStyleUri())
                .basePath(BASE_PATH)
                .port(host.getPort());
    }

    public RequestSpecification relaxedHttps() {
        return this.https().relaxedHTTPSValidation();
    }

    public WebClient mutiny() {
        return mutiny(new WebClientOptions());
    }

    public WebClient mutiny(WebClientOptions options) {
        if (webClient == null) {
            var uri = getURI(Protocol.HTTP);
            webClient = WebClient.create(Vertx.vertx(), options
                    .setDefaultHost(uri.getHost())
                    .setDefaultPort(uri.getPort()));
        }

        return webClient;
    }

    /**
     * @see this#mutinyHttps(boolean, String, boolean)
     */
    public WebClient mutinyHttps() {
        return mutinyHttps(null);
    }

    /**
     * @see this#mutinyHttps(boolean, String, boolean)
     */
    public WebClient mutinyHttps(String clientCertificateCn) {
        return mutinyHttps(true, clientCertificateCn, true);
    }

    /**
     * Returns {@link WebClient} configured from {@link CertificateBuilder}.
     * The SSL configuration is done on best effort basis and should only work for JKS and PKCS12 formats.
     * When specific configuration is required, manual client configuration is better option.
     * This HTTPS version of {@link WebClient} is not cached, however it is automatically closed for you.
     * If you need to reuse a client with same configuration within one test class, just keep it in a test class var.
     * Default port and host are always preconfigured for you.
     * If management interface SSL is enabled, the defaults are configured to the management interface.
     * You can always declare your own host and port during the request.
     */
    public WebClient mutinyHttps(boolean verifyHost, String clientCertificateCn, boolean withTruststore) {
        var certificateBuilder = this.<CertificateBuilder> getPropertyFromContext(CertificateBuilder.INSTANCE_KEY);
        if (certificateBuilder.certificates().size() != 1) {
            throw new IllegalArgumentException("Exactly one certificate must exist for the SSL configuration to work");
        }

        var options = new WebClientOptions();
        options.setVerifyHost(verifyHost);
        options.setSsl(true);

        final URILike uri;
        if (TestExecutionProperties.useManagementSsl(this)) {
            uri = getURI(Protocol.MANAGEMENT);
        } else {
            uri = getURI(Protocol.HTTPS);
        }
        options.setDefaultHost(uri.getHost());
        options.setDefaultPort(uri.getPort());

        final String keystorePath;
        final String truststorePath;
        var certificate = certificateBuilder.certificates().get(0);

        if (clientCertificateCn != null) {
            var clientCert = certificate.getClientCertificateByCn(clientCertificateCn);
            Objects.requireNonNull(clientCert, "Client certificate with CN %s not found".formatted(clientCertificateCn));
            keystorePath = clientCert.keystorePath();
            truststorePath = clientCert.truststorePath();
        } else {
            keystorePath = certificate.keystorePath();
            truststorePath = certificate.truststorePath();
        }

        if (keystorePath != null) {
            options.setKeyCertOptions(
                    new KeyStoreOptions().setValue(Buffer.buffer(getFileContent(keystorePath)))
                            .setPassword(certificate.password()).setType(certificate.format()));
        }

        if (withTruststore && truststorePath != null) {
            options.setTrustOptions(
                    new KeyStoreOptions().setValue(Buffer.buffer(getFileContent(truststorePath)))
                            .setPassword(certificate.password()).setType(certificate.format()));
        }

        var vertx = Vertx.vertx();
        var webClient = WebClient.create(vertx, options);
        onPreStop(service -> {
            webClient.close();
            vertx.close().await().indefinitely();
        });
        return webClient;
    }

    @Override
    public void start() {
        super.start();
        var host = getURI(Protocol.HTTP);
        RestAssured.baseURI = host.getRestAssuredStyleUri();
        RestAssured.basePath = BASE_PATH;
        RestAssured.port = host.getPort();
    }

    private static byte[] getFileContent(String path) {
        byte[] data;
        try (InputStream is = Files.newInputStream(Path.of(path))) {
            data = doRead(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

}
