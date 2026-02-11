package io.quarkus.qe.mcp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.restassured.response.Response;

public abstract class BasicMCPIT {

    abstract RestService getClient();

    @Test
    public void smoke() {
        Response response = getClient().given().get("/mcp/tools");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("[filereader]", response.body().asString());
    }

    @Test
    public void args() {
        Response response = getClient().given().get("/mcp/tools/filereader/arguments");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("[[file]]", response.body().asString());
    }

    @Test
    public void readFile() {
        Response response = getClient().given().body("robot-readable.txt").post("/mcp/readFile");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("Hi, AI!", response.body().asString());
    }
}
