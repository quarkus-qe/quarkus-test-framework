package io.quarkus.qe.funqy.knativeevents;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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
