package io.quarkus.qe.grpc;

import static io.quarkus.test.services.Certificate.Format.ENCRYPTED_PEM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.GrpcService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.Certificate.ClientCertificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GrpcMtlsEncryptedPemTlsRegistryIT {

    private static final String CN = "Hagrid";
    private static final String NAME = "Albus";

    @QuarkusApplication(grpc = true, ssl = true, certificates = @Certificate(format = ENCRYPTED_PEM, configureHttpServer = true, configureKeystore = true, configureTruststore = true, tlsConfigName = "grpc-tls", clientCertificates = @ClientCertificate(cnAttribute = CN)))
    static final GrpcService app = (GrpcService) new GrpcService()
            .withProperty("quarkus.grpc.server.use-separate-server", "false")
            .withProperty("quarkus.http.insecure-requests", "disabled")
            .withProperty("quarkus.http.ssl.client-auth", "request")
            .withProperty("quarkus.http.auth.permission.perm-1.policy", "authenticated")
            .withProperty("quarkus.http.auth.permission.perm-1.paths", "*")
            .withProperty("quarkus.http.auth.permission.perm-1.auth-mechanism", "X509");

    @Test
    public void testMutualTlsCommunicationWithHelloService() {
        try (var channel = app.securedGrpcChannel()) {
            // here both server and client certificates are generated and used
            HiRequest request = HiRequest.newBuilder().setName(NAME).build();
            HiReply response = GreeterGrpc.newBlockingStub(channel).sayHi(request);

            assertEquals("Hello " + NAME, response.getMessage());
            assertEquals("CN=Hagrid", response.getPrincipalName());
        }
    }

}
