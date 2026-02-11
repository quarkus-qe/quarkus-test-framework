package io.quarkus.qe.mcp;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class MCPIT {

    @QuarkusApplication(boms = { @Dependency(artifactId = "quarkus-mcp-server-bom") }, dependencies = {
            @Dependency(groupId = "io.quarkiverse.mcp", artifactId = "quarkus-mcp-server-websocket")
    }, classes = { FileServer.class })
    static final RestService server = new RestService()
            .withProperty("working.folder", () -> Path.of("target").toAbsolutePath().toString());

    @QuarkusApplication(properties = "mcp-client.properties", boms = {
            @Dependency(artifactId = "quarkus-langchain4j-bom") }, dependencies = {
                    @Dependency(artifactId = "quarkus-rest"),
                    @Dependency(groupId = "io.quarkiverse.langchain4j", artifactId = "quarkus-langchain4j-mcp"),
            }, classes = { MCPClient.class })
    static final RestService client = new RestService()
            .withProperty("quarkus.langchain4j.mcp.filesystem.url",
                    () -> server.getURI(Protocol.WS).withPath("/mcp/ws").toString());

    @Test
    public void smoke() {
        Response response = client.given().get("/mcp/tools");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("[filereader]", response.body().asString());
    }

    @Test
    public void args() {
        Response response = client.given().get("/mcp/tools/filereader/arguments");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("[[file]]", response.body().asString());
    }

    @Test
    public void readFile() {
        Response response = client.given().body("robot-readable.txt").post("/mcp/readFile");
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("Hi, AI!", response.body().asString());
    }
}
