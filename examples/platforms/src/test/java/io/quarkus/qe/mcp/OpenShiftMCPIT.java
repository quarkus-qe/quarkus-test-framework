package io.quarkus.qe.mcp;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

@OpenShiftScenario
@Disabled("https://github.com/quarkiverse/quarkus-langchain4j/issues/2340")
public class OpenShiftMCPIT extends BasicMCPIT {

    private static String workingFolder = QuarkusProperties.isNativeEnabled() ? "/home/quarkus" : "/deployments";

    @QuarkusApplication(boms = { @Dependency(artifactId = "quarkus-mcp-server-bom") }, dependencies = {
            @Dependency(groupId = "io.quarkiverse.mcp", artifactId = "quarkus-mcp-server-websocket")
    }, classes = { FileServer.class })
    static final RestService server = new RestService()
            .withProperty("_ignored", "resource_with_destination::" + workingFolder + "|robot-readable.txt")
            .withProperty("working.folder", workingFolder);

    @QuarkusApplication(properties = "mcp-client.properties", boms = {
            @Dependency(artifactId = "quarkus-langchain4j-bom") }, dependencies = {
                    @Dependency(artifactId = "quarkus-rest"),
                    @Dependency(groupId = "io.quarkiverse.langchain4j", artifactId = "quarkus-langchain4j-mcp"),
            }, classes = { MCPClient.class })
    static final RestService client = new RestService()
            .withProperty("quarkus.langchain4j.mcp.filesystem.url",
                    () -> server.getURI(Protocol.WS).withPath("/mcp/ws").toString());

    @Override
    RestService getClient() {
        return client;
    }
}
