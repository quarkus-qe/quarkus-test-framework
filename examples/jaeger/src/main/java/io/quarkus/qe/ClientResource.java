package io.quarkus.qe;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.opentracing.Tracer;

@Path("/client")
public class ClientResource {

    @Inject
    Tracer tracer;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        tracer.activeSpan().log("ClientResource called");
        return "I'm a client";
    }
}
