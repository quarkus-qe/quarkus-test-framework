
package io.quarkus.qe;

import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@DisabledOnNative(reason = "https://github.com/quarkusio/quarkus/issues/57243")
@OpenShiftScenario
public class OpenShiftStrimziKafkaWithRegistryMessagingIT extends StrimziKafkaWithRegistryMessagingIT {

}
