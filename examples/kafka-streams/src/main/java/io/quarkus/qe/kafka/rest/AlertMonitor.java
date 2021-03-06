package io.quarkus.qe.kafka.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
