package io.quarkus.qe;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.scenarios.annotations.EnabledOnQuarkusVersion;
import io.quarkus.test.services.GitRepositoryQuarkusApplication;

@QuarkusScenario
// TODO: remove when Quarkus QuickStarts migrates to Quarkus 3
@EnabledOnQuarkusVersion(version = "999-SNAPSHOT", reason = "QuickStarts on development branch uses 999-SNAPSHOT")
@DisabledOnNative
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows does not support long file paths")
public class DevModeQuickstartUsingDefaultsIT {

    // TODO: switch to main branch when Quarkus QuickStarts migrates to Quarkus 3
    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", branch = "development", contextDir = "getting-started", devMode = true)
    static final RestService app = new RestService();

    @Test
    public void test() {
        app.given()
                .get("/hello")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
