package io.quarkus.qe;

import static io.quarkus.test.bootstrap.KeycloakService.KEYSTORE_PASSWORD;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@Certificate()
public class SecurityResourceIT extends BaseSecurityResourceIT {

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", keycloak::getRealmUrl)
            .withProperty("quarkus.oidc.client-id", CLIENT_ID_DEFAULT)
            .withProperty("quarkus.oidc.credentials.secret", CLIENT_SECRET_DEFAULT)
            .withProperty("quarkus.oidc.tls.tls-configuration-name", "oidc")
            .withProperty("quarkus.tls.oidc.trust-store.jks.path", keycloak.getTrustStoreName())
            .withProperty("quarkus.tls.oidc.trust-store.jks.password", KEYSTORE_PASSWORD);

    @Override
    public RestService getApp() {
        return app;
    }
}
