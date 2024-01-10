package io.quarkus.qe.funqy.knativeevents;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.knative.eventing.FunqyKnativeEventsService;
import io.quarkus.test.services.knative.eventing.OpenShiftExtensionFunqyKnativeEventsService;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
@Disabled("https://github.com/quarkusio/quarkus/issues/38018")
public class OpenShiftUsingExtensionAndServerlessFunqyKnEventsIT {

    private static final String INVOKED_FUNCTION = "defaultChain";
    private static final String DATA = "\"Start\"";
    private static final Matcher<String> EXPECTED_RESULT_MATCHER = equalTo(
            "Start::defaultChain::configChain::annotatedChain::lastChainLink");

    @QuarkusApplication
    static FunqyKnativeEventsService service = new OpenShiftExtensionFunqyKnativeEventsService()
            .withDefaultBroker()
            .withTrigger().name("annotatedchain").defaultBroker().filterCloudEventType("annotated").endTrigger()
            .withTrigger().name("configChain").defaultBroker().filterCloudEventType("defaultChain.output").endTrigger()
            .withTrigger().name("defaultchain").defaultBroker().filterCloudEventType("defaultChain").endTrigger()
            .withTrigger().name("lastchainlink").defaultBroker().filterCloudEventType("lastChainLink").endTrigger();

    @Test
    public void simpleFunctionChainCloudEventGetTest() {
        final String actualResponse = service
                .<String> funcInvoker()
                .cloudEventType(INVOKED_FUNCTION)
                .data(DATA)
                .appCloudEventsPlusJsonContentType()
                .get()
                .getResponse()
                .jsonPath()
                .get("data.response")
                .toString();
        assertTrue(EXPECTED_RESULT_MATCHER.matches(actualResponse));
    }

    @Test
    public void simpleFunctionChainHttpPostTest() {
        service
                .<String> funcInvoker()
                .appJsonContentType()
                .cloudEventType(INVOKED_FUNCTION)
                .data(DATA)
                .post()
                .assertBody(EXPECTED_RESULT_MATCHER);
    }

    @Test
    public void simpleFunctionChainCloudEventTest() {
        service
                .<String> funcInvoker()
                .appJsonContentType()
                .cloudEventType(INVOKED_FUNCTION)
                .data(DATA)
                .asCloudEventObject()
                .post()
                .assertBody(EXPECTED_RESULT_MATCHER);
    }

}
