package io.quarkus.qe;

import io.quarkus.test.bootstrap.ConsulService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;

@QuarkusScenario
public class GreetingResourceIT extends BaseGreetingResourceIT {

    /**
     * The framework will try to resolve the property `property.do.not.exist`
     * and as it does not exist, it will use the default image.
     */
    @Container(image = "${property.do.not.exist:docker.io/consul:1.9.17}", expectedLog = "Synced node info", port = 8500)
    static ConsulService consul = new ConsulService().onPostStart(BaseGreetingResourceIT::onLoadConfigureConsul);
}
