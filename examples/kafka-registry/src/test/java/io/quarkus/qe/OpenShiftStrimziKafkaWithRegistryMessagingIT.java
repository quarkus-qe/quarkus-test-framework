
package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@DisabledOnQuarkusVersion(version = "(3\\.[0]\\..*)", reason = "Breaking change in Quarkus 3.1 - Apicurio Rest Client is not compatible with Apicurio Registry 2.2.5.Final")
@OpenShiftScenario
public class OpenShiftStrimziKafkaWithRegistryMessagingIT extends StrimziKafkaWithRegistryMessagingIT {

}
