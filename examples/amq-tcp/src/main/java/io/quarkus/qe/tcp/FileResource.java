package io.quarkus.qe.tcp;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/file")
public class FileResource {

    @GET
    @Path("/custom/text.txt")
    @Produces(MediaType.TEXT_PLAIN)
    public String text() throws IOException {
        return new String(FileResource.class.getResourceAsStream("/custom/text.txt").readAllBytes());
    }

    @GET
    @Path("/custom.cc")
    @Produces(MediaType.TEXT_PLAIN)
    public String cc() throws IOException {
        return new String(FileResource.class.getResourceAsStream("/custom.cc").readAllBytes());
    }
}
