package io.quarkus.qe.mcp;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.quarkiverse.langchain4j.mcp.runtime.McpClientName;

@Path("/mcp")
public class MCPClient {
    @Inject
    @McpClientName("filesystem")
    McpClient filesystemClient;

    @GET
    @Path("/tools")
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> tools() {
        return filesystemClient.listTools().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/tools/{name}/arguments")
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> arguments(@PathParam("name") String name) {
        return filesystemClient.listTools().stream()
                .filter(spec -> spec.name().equals(name))
                .map(ToolSpecification::parameters)
                .map(jsonObjectSchema -> jsonObjectSchema.properties().keySet().toString())
                .collect(Collectors.toList());
    }

    @POST
    @Path("/readFile")
    @Produces(MediaType.TEXT_PLAIN)
    public String readFile(String file) {
        ToolExecutionResult result = filesystemClient.executeTool(ToolExecutionRequest.builder()
                .name("filereader")
                .arguments("""
                        {"file":"%s"}
                        """.formatted(file))
                .build());
        return result.resultText();
    }

}
