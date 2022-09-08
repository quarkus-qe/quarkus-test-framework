package io.quarkus.qe.books;

import io.quarkus.test.bootstrap.InfinispanService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class UsingJksInfinispanBookCacheIT extends BaseBookCacheIT {

    @Container(image = "docker.io/infinispan/server:13.0", expectedLog = "Infinispan Server.*started in", port = 11222)
    static final InfinispanService infinispan = new InfinispanService()
            .withConfigFile("jks-config.yaml")
            .withSecretFiles("jks/server.jks");

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.infinispan-client.server-list", infinispan::getInfinispanServerAddress)
            .withProperty("quarkus.infinispan-client.auth-username", infinispan.getUsername())
            .withProperty("quarkus.infinispan-client.auth-password", infinispan.getPassword())
            .withProperty("quarkus.infinispan-client.trust-store", "secret::/jks/server.jks")
            .withProperty("quarkus.infinispan-client.trust-store-password", "changeit")
            .withProperty("quarkus.infinispan-client.trust-store-type", "jks");
}
