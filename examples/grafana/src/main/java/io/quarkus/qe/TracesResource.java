package io.quarkus.qe;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.opentelemetry.api.trace.Span;

@Path("/traces")
public class TracesResource {

    @Inject
    Span span;

    @Path("/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTraceId(@PathParam("name") String name) {
        return span.getSpanContext().getTraceId();
    }
}
