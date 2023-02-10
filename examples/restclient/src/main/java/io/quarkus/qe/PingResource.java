package io.quarkus.qe;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/ping")
public class PingResource {

    @Inject
    @RestClient
    PongClient pongClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "ping";
    }

    @Path("/pong")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pingPong() {
        return "ping" + pongClient.getPong();
    }
}
