package io.quarkus.qe;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient
@Path("/pong")
public interface PongClient {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getPong();
}
