package io.quarkus.qe.grpc;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.qe.grpc.test.rest.HiResource;
import io.quarkus.test.bootstrap.GrpcService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
public class DevModeForcedDependencyIT {

    public static final String NAME = "Victor";

    @DevModeQuarkusApplication(grpc = true, classes = {
            HelloService.class,
            HiResource.class
    }, dependencies = @Dependency(artifactId = "quarkus-rest"))
    static final GrpcService app = new GrpcService();

    @Test
    public void testGrpcHelloWorldServiceWork() {
        try (var channel = app.grpcChannel()) {
            HelloRequest request = HelloRequest.newBuilder().setName(NAME).build();
            HelloReply response = GreeterGrpc.newBlockingStub(channel).sayHello(request);

            assertEquals("Hello " + NAME, response.getMessage());
        }
    }

    @Test
    public void testRestHiResourceWork() {
        app.given().get("/hi").then().statusCode(200).body(is("Hi " + NAME));
    }
}
