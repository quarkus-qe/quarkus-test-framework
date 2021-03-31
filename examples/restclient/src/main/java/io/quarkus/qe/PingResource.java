package io.quarkus.qe;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
