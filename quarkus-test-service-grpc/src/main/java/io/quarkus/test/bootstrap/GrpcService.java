package io.quarkus.test.bootstrap;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.net.ssl.SSLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.test.security.certificate.CertificateBuilder;
import io.quarkus.test.security.certificate.PemClientCertificate;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public class GrpcService extends RestService {

    public CloseableManagedChannel grpcChannel() {
        var nettyChannel = NettyChannelBuilder.forAddress(getGrpcHost().getHost(), getGrpcHost().getPort()).usePlaintext()
                .build();
        return new CloseableManagedChannel(nettyChannel);
    }

    public CloseableManagedChannel securedGrpcChannel() {
        return securedGrpcChannel(null);
    }

    public CloseableManagedChannel securedGrpcChannel(String clientCnName) {
        if (QuarkusProperties.useSeparateGrpcServer(context)) {
            throw new IllegalStateException("TLS is not currently supported for a separate gRPC server");
        }
        var httpsUri = getURI(Protocol.HTTPS);
        var nettyChannel = NettyChannelBuilder.forAddress(httpsUri.getHost(), httpsUri.getPort())
                .sslContext(createSslContext(clientCnName))
                .useTransportSecurity()
                .build();
        return new CloseableManagedChannel(nettyChannel);
    }

    private SslContext createSslContext(String clientCnName) {
        SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();

        // only implementing this for one certificate and one client certificate for we don't need more ATM
        CertificateBuilder certBuilder = getPropertyFromContext(CertificateBuilder.INSTANCE_KEY);

        if (certBuilder.certificates().size() != 1) {
            throw new IllegalStateException(
                    """
                            Exactly one certificate must exist for the SSL configuration to work, found '%2$s'.
                            You can inspect existing certificates found in the '%1$s' yourself.
                            The '%1$s' is available in the ServiceContext under key '%3$s'.
                            """.formatted(CertificateBuilder.class.getName(), certBuilder.certificates().size(),
                            CertificateBuilder.INSTANCE_KEY));
        }
        var certificate = certBuilder.certificates().get(0);

        var clientCertificates = certificate.clientCertificates();
        if (clientCertificates.isEmpty()) {
            if (clientCnName != null) {
                throw new IllegalArgumentException("No client certificate found for CN: " + clientCnName);
            }
            sslContextBuilder.trustManager(new File(certificate.truststorePath()));
        } else {
            // mTLS scenario
            sslContextBuilder.clientAuth(ClientAuth.REQUIRE);

            final PemClientCertificate clientCert;
            if (clientCnName != null) {
                clientCert = (PemClientCertificate) certificate.getClientCertificateByCn(clientCnName);
                if (clientCert == null) {
                    throw new IllegalArgumentException("No client certificate found for CN: " + clientCnName);
                }
            } else {
                if (clientCertificates.size() > 1) {
                    throw new IllegalStateException("Only one client certificate is allowed");
                }
                if (clientCertificates.stream().findFirst().get() instanceof PemClientCertificate clientPemCert) {
                    clientCert = clientPemCert;
                } else {
                    throw new IllegalStateException("Only PEM format is supported for client certificates");
                }
            }
            sslContextBuilder.trustManager(new File(clientCert.truststorePath()));
            if (clientCert.isEncrypted()) {
                try (var keyFile = IOUtils.toInputStream(clientCert.loadAndDecryptKeyCertificate().toString(), UTF_8);
                        var certFile = FileUtils.openInputStream(new File(clientCert.certPath()))) {
                    sslContextBuilder.keyManager(certFile, keyFile);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create SSL context", e);
                }
            } else {
                var certFile = new File(clientCert.certPath());
                var keyFile = new File(clientCert.keyPath());
                sslContextBuilder.keyManager(certFile, keyFile);
            }
        }

        Objects.requireNonNull(certBuilder, """
                    Requested gRPC secured channel cannot be configured without the 'CertificateBuilder' in test context.
                    Use the '@Certificate' annotation to configure the 'CertificateBuilder'.
                """);
        try {
            return sslContextBuilder.build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    public URILike getGrpcHost() {
        return getURI(Protocol.GRPC);
    }

}
