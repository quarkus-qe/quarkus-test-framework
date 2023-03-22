package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ping")
public class HttpResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pong() {
        return "pong";
    }
}
