package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
