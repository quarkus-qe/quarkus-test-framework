package io.quarkus.qe.kafka.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import io.quarkus.qe.kafka.streams.WindowedLoginDeniedStream;

@Path("/monitor")
public class AlertMonitor {

    @Inject
    @Channel(WindowedLoginDeniedStream.LOGIN_ALERTS_TOPIC)
    Publisher<String> alerts;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType("application/json")
    public Publisher<String> stream() {
        return alerts;
    }

}
