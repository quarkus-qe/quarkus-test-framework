package io.quarkus.qe.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
public class DevModeMCPIT extends BasicMCPIT {

    @DevModeQuarkusApplication(boms = { @Dependency(artifactId = "quarkus-mcp-server-bom") }, dependencies = {
            @Dependency(groupId = "io.quarkiverse.mcp", artifactId = "quarkus-mcp-server-websocket")
    }, classes = { FileServer.class })
    static final RestService server = new RestService()
            .withProperty("working.folder", () -> Path.of("target").toAbsolutePath().toString());

    @DevModeQuarkusApplication(properties = "mcp-client.properties", boms = {
            @Dependency(artifactId = "quarkus-langchain4j-bom") }, dependencies = {
                    @Dependency(artifactId = "quarkus-rest"),
                    @Dependency(groupId = "io.quarkiverse.langchain4j", artifactId = "quarkus-langchain4j-mcp"),
            }, classes = { MCPClient.class })
    static final RestService client = new RestService()
            .withProperty("quarkus.langchain4j.mcp.filesystem.url",
                    () -> server.getURI(Protocol.WS).withPath("/mcp/ws").toString());

    @BeforeEach
    // for some reason, the file is not copied automatically for dev mode tests
    void setUp() throws IOException {
        Path source = Path.of("target/test-classes/robot-readable.txt").toAbsolutePath();
        Path target = server.getServiceFolder().resolve("robot-readable.txt").toAbsolutePath();
        if (!Files.exists(target)) {
            Files.copy(source, target);
        }
    }

    @Override
    RestService getClient() {
        return client;
    }
}
