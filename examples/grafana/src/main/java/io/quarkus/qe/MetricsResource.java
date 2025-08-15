package io.quarkus.qe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

@Path("/metrics")
public class MetricsResource {

    private final LongCounter counter;

    public MetricsResource(Meter meter) {
        counter = meter.counterBuilder("hello-metrics")
                .setDescription("hello-metrics")
                .setUnit("invocations")
                .build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        counter.add(1);
        return "hello-metrics";
    }
}
