package io.quarkus.qe.funqy.knativeevents;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.test.services.knative.eventing.spi.CompoundResponse.StringCompoundResponse;
import io.quarkus.test.services.knative.eventing.spi.ForwardRequestDTO;
import io.quarkus.test.services.knative.eventing.spi.ForwardResponseDTO;

/**
 * Funqy Knative Events Quickstart with additional function that forwards payload to the broker and collect responses.
 */
public class SimpleFunctionChain {
    private static final Logger LOG = Logger.getLogger(SimpleFunctionChain.class);
    private static final int EXPECTED_NUMBER_OF_RESPONSES = 4;
    private static volatile StringCompoundResponse compoundResponse = null;

    @RestClient
    BrokerClient brokerClient;

    /**
     * Expects knative event of type "defaultChain". Creates event of type "defaultChain.output".
     *
     * This function is triggered by an external curl invocation.
     *
     * @param input
     * @return
     */
    @Funq
    public String defaultChain(String input) {
        LOG.info("*** defaultChain ***");
        compoundResponse.recordVisit();
        return input + "::" + "defaultChain";
    }

    /**
     * This is triggered by defaultChain and is example of using application.properties to
     * map the cloud event to this function and to map response. Response will trigger
     * the annotatedChain function.
     *
     * @param input
     * @return
     */
    @Funq
    public String configChain(String input) {
        LOG.info("*** configChain ***");
        compoundResponse.recordVisit();
        return input + "::" + "configChain";
    }

    /**
     * Triggered by configChain output.
     *
     * Example of mapping the cloud event via an annotation.
     *
     * @param input
     * @return
     */
    @Funq
    @CloudEventMapping(trigger = "annotated", responseSource = "annotated", responseType = "lastChainLink")
    public String annotatedChain(String input) {
        LOG.info("*** annotatedChain ***");
        compoundResponse.recordVisit();
        return input + "::" + "annotatedChain";
    }

    /**
     * Last event in chain. Has no output. Triggered by annotatedChain.
     *
     * @param input
     */
    @Funq
    public void lastChainLink(String input, @Context CloudEvent event) {
        LOG.info("*** lastChainLink ***");
        compoundResponse.recordResponse(input + "::" + "lastChainLink");
    }

    /**
     * Forward event to the broker and wait for chain responses.
     */
    @Funq
    public ForwardResponseDTO<String> clusterEntrypoint(ForwardRequestDTO<String> requestDTO) {
        compoundResponse = new StringCompoundResponse(EXPECTED_NUMBER_OF_RESPONSES);
        brokerClient.forwardEventToBroker(requestDTO.getFilterCloudEventType(), requestDTO.getData());
        return compoundResponse.waitForResponses().join();
    }

}
