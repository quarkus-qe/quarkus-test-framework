package io.quarkus.qe.mcp;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
public class OpenShiftMCPIT extends BasicMCPIT {

    @QuarkusApplication(boms = { @Dependency(artifactId = "quarkus-mcp-server-bom") }, dependencies = {
            @Dependency(groupId = "io.quarkiverse.mcp", artifactId = "quarkus-mcp-server-websocket")
    }, classes = { FileServer.class })
    static final RestService server = new RestService()
            .withProperty("_ignored", "resource_with_destination::/deployments|robot-readable.txt")
            .withProperty("working.folder", "/deployments");

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
