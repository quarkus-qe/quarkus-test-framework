package io.quarkus.qe.database.mysql;

import java.util.Objects;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.MySqlService;
import io.quarkus.test.services.Container;

public abstract class AbstractMysqlReusableInstance extends AbstractSqlDatabaseIT {

    /**
     * This mysql instance will be shared between different @QuarkusScenarios if
     * property `ts.database.container.reusable` is enabled on test.properties
     **/

    @Container(image = "${mysql.image}", port = 3306, expectedLog = "port: 3306  MySQL Community Server")
    public static MySqlService database = new MySqlService();

    static Integer containerPort;

    protected void IsContainerReused() {
        if (isFirstInstance()) {
            setContainerPort();
        }

        Assertions.assertEquals(containerPort.intValue(), database.getURI().getPort());
    }

    private boolean isFirstInstance() {
        return Objects.isNull(containerPort);
    }

    private void setContainerPort() {
        containerPort = database.getURI().getPort();
        ;
    }
}
