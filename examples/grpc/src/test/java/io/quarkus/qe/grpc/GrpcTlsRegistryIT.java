package io.quarkus.qe.grpc;

import static io.quarkus.test.services.Certificate.Format.PEM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import io.quarkus.test.bootstrap.GrpcService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GrpcTlsRegistryIT {

    private static final String NAME = "Albus";

    @QuarkusApplication(grpc = true, ssl = true, certificates = @Certificate(format = PEM, configureHttpServer = true, configureKeystore = true, configureTruststore = true, tlsConfigName = "grpc-tls"))
    static final GrpcService app = (GrpcService) new GrpcService()
            .withProperty("quarkus.grpc.server.use-separate-server", "false")
            .withProperty("quarkus.http.insecure-requests", "disabled");

    @Test
    public void testGrpcServiceUsingTls() {
        try (var channel = app.securedGrpcChannel()) {
            HiRequest request = HiRequest.newBuilder().setName(NAME).build();
            HiReply response = GreeterGrpc.newBlockingStub(channel).sayHi(request);

            assertEquals("Hello " + NAME, response.getMessage());
            // no authentication
            assertEquals("", response.getPrincipalName());
        }
    }

    @Test
    public void testUsingTlsIsRequired() {
        try (var channel = app.grpcChannel()) {
            var greeterGrpcStub = GreeterGrpc.newBlockingStub(channel);
            HiRequest request = HiRequest.newBuilder().setName(NAME).build();
            Assertions.assertThrows(StatusRuntimeException.class, () -> greeterGrpcStub.sayHi(request),
                    "Secured channel should be required but isn't");
        }
    }

}
