package io.quarkus.qe;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/pong")
public class PongResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pong() {
        return "pong";
    }
}
