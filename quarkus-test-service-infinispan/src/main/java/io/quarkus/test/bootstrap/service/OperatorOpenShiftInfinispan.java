package io.quarkus.test.bootstrap.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OperatorOpenShiftInfinispan {
    String clientCertSecret();

    String clusterConfig();

    String clusterConfigMap();

    String connectSecret();

    String tlsSecret();

    String clusterNameSpace() default "datagrid-cluster";

    String templateClusterNameSpace() default "totally-random-infinispan-cluster-name";

    String templateTlsSecretName() default "tls-secret";

    String templateConnectSecretName() default "connect-secret";
}
