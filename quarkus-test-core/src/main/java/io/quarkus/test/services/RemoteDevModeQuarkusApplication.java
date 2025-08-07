package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.services.IsRunningCheck.AlwaysFail;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteDevModeQuarkusApplication {
    String password() default "qe";

    /**
     * @return the properties file to use to configure the Quarkus application.
     */
    String properties() default "application.properties";

    /**
     * Please note that this feature is currently implemented for bare-metal scenarios only and the check will be
     * ignored for OpenShift and Kubernetes scenarios.
     *
     * @return {@link IsRunningCheck} that can mark the service as running even if default log-based check fails.
     */
    Class<? extends IsRunningCheck> isRunningCheck() default AlwaysFail.class;

}
