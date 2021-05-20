package io.quarkus.test.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ThrowingRunnable;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import io.quarkus.test.logging.Log;

/**
 * Awaitility utils to make a long or repeatable operation.
 */
public final class AwaitilityUtils {

    private static final int POLL_SECONDS = 1;
    private static final int TIMEOUT_SECONDS = 30;

    private AwaitilityUtils() {

    }

    /**
     * Wait until supplier returns a not null instance.
     *
     * @param supplier method to return the instance.
     * @return the non null instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T untilIsNotNull(Supplier<T> supplier) {
        return until(supplier, (Matcher<T>) Matchers.notNullValue());
    }

    /**
     * Wait until supplier returns a not empty array.
     *
     * @param supplier method to return the instance.
     * @return the non empty array.
     */
    public static <T> T[] untilIsNotEmpty(Supplier<T[]> supplier) {
        return until(supplier, Matchers.arrayWithSize(Matchers.greaterThan(0)));
    }

    /**
     * Wait until the supplier returns an instance that satisfies the asserts.
     *
     * @param supplier method to return the instance.
     * @param asserts custom assertions that the instance must satisfy.
     */
    public static <T> void untilAsserted(Supplier<T> supplier, Consumer<T> asserts) {
        awaits().untilAsserted(() -> asserts.accept(get(supplier).call()));
    }

    /**
     * Wait until the assertions are satified.
     *
     * @param assertion custom assertions that the instance must satisfy.
     */
    public static void untilAsserted(final ThrowingRunnable assertion) {
        awaits().untilAsserted(assertion);
    }

    public static <T> T until(Supplier<T> supplier, Matcher<T> matcher) {
        return awaits().until(get(supplier), matcher);
    }

    private static <T> Callable<T> get(Supplier<T> supplier) {
        return () -> {
            T instance = supplier.get();
            Log.debug("Checking ... {}", instance);
            return instance;
        };
    }

    private static ConditionFactory awaits() {
        return Awaitility.await()
                .ignoreExceptions()
                .pollInterval(POLL_SECONDS, TimeUnit.SECONDS)
                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
