package io.quarkus.test.services.containers;

import static io.quarkus.test.services.containers.KeycloakGenericDockerContainerManagedResource.convertMiBtoBytes;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

public class MemoryLimitTest {

    @Test
    void convertMiBToBytes() {
        assertThat(convertMiBtoBytes(0), CoreMatchers.is(0L));
        assertThat(convertMiBtoBytes(-0), CoreMatchers.is(0L));

        assertThat(convertMiBtoBytes(1000L), CoreMatchers.is(1048576000L));
        assertThat(convertMiBtoBytes(1000), CoreMatchers.is(1048576000L));
        assertThat(convertMiBtoBytes(100), CoreMatchers.is(104857600L));
        assertThat(convertMiBtoBytes(10), CoreMatchers.is(10485760L));
        assertThat(convertMiBtoBytes(1), CoreMatchers.is(1048576L));

        assertThat(convertMiBtoBytes(-1000L), CoreMatchers.is(-1048576000L));
        assertThat(convertMiBtoBytes(-1000), CoreMatchers.is(-1048576000L));
        assertThat(convertMiBtoBytes(-100), CoreMatchers.is(-104857600L));
        assertThat(convertMiBtoBytes(-10), CoreMatchers.is(-10485760L));
        assertThat(convertMiBtoBytes(-1), CoreMatchers.is(-1048576L));

        assertThat(convertMiBtoBytes(2000), CoreMatchers.is(2097152000L));
        assertThat(convertMiBtoBytes(20000), CoreMatchers.is(20971520000L));
        assertThat(convertMiBtoBytes(200000), CoreMatchers.is(209715200000L));

        assertThat(convertMiBtoBytes(999999999999999999L), CoreMatchers.is(Long.MAX_VALUE));

        assertThat(convertMiBtoBytes(Integer.MAX_VALUE), CoreMatchers.is((long) Integer.MAX_VALUE << 20));
        assertThat(convertMiBtoBytes(Integer.MIN_VALUE), CoreMatchers.is((long) Integer.MIN_VALUE << 20));

        assertThat(convertMiBtoBytes(Long.MAX_VALUE), CoreMatchers.is(Long.MAX_VALUE));
        assertThat(convertMiBtoBytes(Long.MIN_VALUE), CoreMatchers.is(Long.MIN_VALUE));
    }
}
