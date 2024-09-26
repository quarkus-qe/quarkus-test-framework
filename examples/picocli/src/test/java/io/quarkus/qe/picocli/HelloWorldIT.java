package io.quarkus.qe.picocli;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class HelloWorldIT {

    static final String NAME = "Random";

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.args", "helloWorld -n " + NAME);

    @Test
    public void verifyHelloWorldFormatted() {
        String expectedOutput = String.format("Hello %s!", NAME);
        app.logs().assertContains(expectedOutput);
    }
}
