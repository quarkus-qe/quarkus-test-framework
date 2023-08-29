package io.quarkus.test.bootstrap;

import java.lang.reflect.Field;

public interface AnnotationBinding {
    boolean isFor(Field field);

    ManagedResourceBuilder createBuilder(Field field) throws Exception;

    /**
     * Whether associated managed resource requires Linux containers when run on bare metal instances.
     *
     * @return true if there is possibility Linux containers are going to be started
     */
    boolean requiresLinuxContainersOnBareMetal();
}
