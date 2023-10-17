package io.quarkus.qe.books;

import io.quarkus.test.bootstrap.InfinispanService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class UsingJksInfinispanBookCacheIT extends BaseBookCacheIT {

    @Container(image = "docker.io/infinispan/server:14.0", expectedLog = "Infinispan Server.*started in", port = 11222)
    static final InfinispanService infinispan = new InfinispanService()
            .withConfigFile("infinispan.xml")
            .withSecretFiles("/jks/keystore.jks");

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.infinispan-client.hosts", infinispan::getInfinispanServerAddress)
            .withProperty("quarkus.infinispan-client.username", infinispan.getUsername())
            .withProperty("quarkus.infinispan-client.password", infinispan.getPassword())
            .withProperty("quarkus.infinispan-client.trust-store", "secret::/jks/truststore.jks")
            .withProperty("quarkus.infinispan-client.trust-store-password", "password")
            .withProperty("quarkus.infinispan-client.trust-store-type", "jks")
            .withProperty("quarkus.infinispan-client.sni-host-name", "localhost");
}
