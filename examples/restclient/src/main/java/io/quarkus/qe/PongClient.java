package io.quarkus.qe;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/pong")
public interface PongClient {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String getPong();
}
