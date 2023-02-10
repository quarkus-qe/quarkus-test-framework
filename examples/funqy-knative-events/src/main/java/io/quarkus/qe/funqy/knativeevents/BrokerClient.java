package io.quarkus.qe.funqy.knativeevents;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * BrokerClient enables sending of events to the broker.
 */
@Consumes("application/json")
@Produces("application/json")
@Path("/")
@RegisterRestClient(configKey = "broker")
@RegisterClientHeaders(BrokerClient.RequestUUIDHeaderFactory.class)
public interface BrokerClient {

    @POST
    @ClientHeaderParam(name = "ce-specversion", value = "1.0")
    @ClientHeaderParam(name = "ce-source", value = "test")
    void forwardEventToBroker(@HeaderParam("ce-type") String ceType, String data);

    /**
     * Adds a unique id of the event as 'ce-id' header parameter.
     */
    @ApplicationScoped
    class RequestUUIDHeaderFactory implements ClientHeadersFactory {

        @Override
        public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                MultivaluedMap<String, String> clientOutgoingHeaders) {
            MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
            clientOutgoingHeaders.forEach(result::addAll);
            result.add("ce-id", UUID.randomUUID().toString());
            return result;
        }
    }
}
