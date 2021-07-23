package io.quarkus.qe.tcp;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
