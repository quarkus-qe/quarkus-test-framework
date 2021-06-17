package io.quarkus.qe;

import io.quarkus.test.bootstrap.ConsulService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;

@QuarkusScenario
public class GreetingResourceIT extends BaseGreetingResourceIT {

    @Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
    static ConsulService consul = new ConsulService().onPostStart(BaseGreetingResourceIT::onLoadConfigureConsul);
}
