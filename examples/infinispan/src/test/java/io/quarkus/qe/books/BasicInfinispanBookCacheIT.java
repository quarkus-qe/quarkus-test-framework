package io.quarkus.qe.books;

import io.quarkus.test.bootstrap.InfinispanService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class BasicInfinispanBookCacheIT extends BaseBookCacheIT {

    @Container(image = "docker.io/infinispan/server:12.1", expectedLog = "Infinispan Server.*started in", port = 11222)
    static final InfinispanService infinispan = new InfinispanService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.infinispan-client.server-list", infinispan::getInfinispanServerAddress)
            .withProperty("quarkus.infinispan-client.auth-username", infinispan.getUsername())
            .withProperty("quarkus.infinispan-client.auth-password", infinispan.getPassword());
}
