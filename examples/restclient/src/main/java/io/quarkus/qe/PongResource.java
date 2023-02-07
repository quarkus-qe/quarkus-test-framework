package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/pong")
public class PongResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pong() {
        return "pong";
    }
}
