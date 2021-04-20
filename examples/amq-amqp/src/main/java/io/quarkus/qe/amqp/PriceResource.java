package io.quarkus.qe.amqp;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class PriceResource {

    @Inject
    PriceConsumer c;

    @GET
    @Path("/price")
    @Produces(MediaType.TEXT_PLAIN)
    public Response price() {
        return Response.ok().entity(c.getPrices()).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String root() {
        return "All good.";
    }
}
