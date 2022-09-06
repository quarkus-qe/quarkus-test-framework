package io.quarkus.qe;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.opentelemetry.api.trace.Span;

@Path("/client")
public class ClientResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        Span.current().addEvent("ClientResource called");
        return "I'm a client";
    }
}
