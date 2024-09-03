package io.quarkus.qe.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.GrpcService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GrpcServiceIT {

    static final String NAME = "Victor";

    @QuarkusApplication(grpc = true)
    static final GrpcService app = new GrpcService();

    @Test
    public void shouldHelloWorldServiceWork() {
        try (var channel = app.grpcChannel()) {
            HelloRequest request = HelloRequest.newBuilder().setName(NAME).build();
            HelloReply response = GreeterGrpc.newBlockingStub(channel).sayHello(request);

            assertEquals("Hello " + NAME, response.getMessage());
        }
    }

}
