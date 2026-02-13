package io.quarkus.test.utils;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.ThrowingRunnable;
import org.awaitility.core.TimeoutEvent;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;

import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.logging.Log;

/**
 * Awaitility utils to make a long or repeatable operation.
 */
public final class AwaitilityUtils {
    private static final Logger LOG = Logger.getLogger(AwaitilityUtils.class);

    private static final int POLL_SECONDS = 1;
    private static final int TIMEOUT_SECONDS = 30;

    private AwaitilityUtils() {

    }

    /**
     * Wait until supplier returns false.
     *
     * @param supplier method to return the instance.
     */
    @SuppressWarnings("unchecked")
    public static void untilIsFalse(Callable<Boolean> supplier) {
        untilIsFalse(supplier, AwaitilitySettings.defaults());
    }

    /**
     * Wait until supplier returns false.
     *
     * @param supplier method to return the instance.
     */
    @SuppressWarnings("unchecked")
    public static void untilIsFalse(Callable<Boolean> supplier, AwaitilitySettings settings) {
        awaits(settings).until(() -> !supplier.call());
    }

    /**
     * Wait until supplier returns false and exit afterwards.
     * See https://github.com/awaitility/awaitility/issues/48 for details
     *
     * @param supplier method to return the instance.
     */
    public static void tryUntilIsFalse(Callable<Boolean> supplier) {
        tryUntilIsFalse(supplier, AwaitilitySettings.defaults());
    }

    /**
     * Wait until supplier returns false and exit afterwards.
     * See https://github.com/awaitility/awaitility/issues/48 for details
     *
     * @param supplier method to return the instance.
     */
    public static void tryUntilIsFalse(Callable<Boolean> supplier, AwaitilitySettings settings) {
        try {
            awaits(settings).until(() -> !supplier.call());
        } catch (ConditionTimeoutException e) {
            LOG.warn("Condition timed out!", e);
        }
    }

    /**
     * Wait until supplier returns true.
     *
     * @param supplier method to return the instance.
     */
    @SuppressWarnings("unchecked")
    public static void untilIsTrue(Callable<Boolean> supplier) {
        untilIsTrue(supplier, AwaitilitySettings.defaults());
    }

    /**
     * Wait until supplier returns true.
     *
     * @param supplier method to return the instance.
     */
    @SuppressWarnings("unchecked")
    public static void untilIsTrue(Callable<Boolean> supplier, AwaitilitySettings settings) {
        awaits(settings).until(supplier);
    }

    /**
     * Wait until supplier returns a not null instance.
     *
     * @param supplier method to return the instance.
     * @return the non null instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T untilIsNotNull(Supplier<T> supplier) {
        return untilIsNotNull(supplier, AwaitilitySettings.defaults());
    }

    /**
     * Wait until supplier returns a not null instance.
     *
     * @param supplier method to return the instance.
     * @param settings Awaitility Settings
     * @return the non null instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T untilIsNotNull(Supplier<T> supplier, AwaitilitySettings settings) {
        return until(supplier, (Matcher<T>) Matchers.notNullValue(), settings);
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
    public static void untilAsserted(ThrowingRunnable assertion) {
        untilAsserted(assertion, AwaitilitySettings.defaults());
    }

    /**
     * Wait until the assertions are satified.
     *
     * @param assertion custom assertions that the instance must satisfy.
     * @param settings Awaitility Settings
     */
    public static void untilAsserted(ThrowingRunnable assertion, AwaitilitySettings settings) {
        awaits(settings).untilAsserted(assertion);
    }

    public static <T> T until(Supplier<T> supplier, Matcher<T> matcher) {
        return until(supplier, matcher, AwaitilitySettings.defaults());
    }

    public static <T> T until(Supplier<T> supplier, Matcher<T> matcher, AwaitilitySettings settings) {
        return awaits(settings).until(get(supplier), matcher);
    }

    private static <T> Callable<T> get(Supplier<T> supplier) {
        return () -> {
            T instance = supplier.get();
            Log.debug("Checking ... {}", instance);
            return instance;
        };
    }

    private static ConditionFactory awaits() {
        return awaits(AwaitilitySettings.defaults());
    }

    private static ConditionFactory awaits(AwaitilitySettings settings) {
        ConditionFactory factory = Awaitility.await()
                .pollInterval(settings.interval.toSeconds(), TimeUnit.SECONDS)
                .atMost(timeoutInSeconds(settings), TimeUnit.SECONDS);

        if (!settings.doNotIgnoreExceptions) {
            factory = factory.ignoreExceptions();
        }

        if (settings.service != null || StringUtils.isNotEmpty(settings.timeoutMessage)) {
            // Enable logging
            factory = factory.conditionEvaluationListener(new CustomConditionEvaluationListener(settings));
        }

        return factory;
    }

    private static long timeoutInSeconds(AwaitilitySettings settings) {
        double timeoutFactor = 1.0;
        if (settings.service != null) {
            timeoutFactor = settings.service.getConfiguration()
                    .getAsDouble(Configuration.Property.TIMEOUT_FACTOR_PROPERTY, timeoutFactor);
        }

        return Math.round(settings.timeout.toSeconds() * timeoutFactor);
    }

    public static final class CustomConditionEvaluationListener implements ConditionEvaluationListener {

        final AwaitilitySettings settings;

        CustomConditionEvaluationListener(AwaitilitySettings settings) {
            this.settings = settings;
        }

        @Override
        public void conditionEvaluated(EvaluatedCondition condition) {
            if (settings.service != null) {
                Log.debug(settings.service, condition.getDescription());
            } else {
                Log.debug(condition.getDescription());
            }
        }

        @Override
        public void onTimeout(TimeoutEvent timeoutEvent) {
            String message = timeoutEvent.getDescription();
            if (StringUtils.isNotEmpty(message)) {
                message = settings.timeoutMessage;
            }

            if (settings.service != null) {
                Log.warn(settings.service, message);
            } else {
                Log.warn(message);
            }
        }
    }

    public static final class AwaitilitySettings {

        Duration interval = Duration.ofSeconds(POLL_SECONDS);
        Duration timeout = Duration.ofSeconds(TIMEOUT_SECONDS);
        Service service;
        String timeoutMessage = StringUtils.EMPTY;
        boolean doNotIgnoreExceptions = false;

        public AwaitilitySettings withService(Service service) {
            this.service = service;
            return this;
        }

        public AwaitilitySettings timeoutMessage(String message, Object... args) {
            this.timeoutMessage = String.format(message, args);
            return this;
        }

        public AwaitilitySettings doNotIgnoreExceptions() {
            this.doNotIgnoreExceptions = true;
            return this;
        }

        public static AwaitilitySettings defaults() {
            return new AwaitilitySettings();
        }

        public static AwaitilitySettings usingTimeout(Duration timeout) {
            AwaitilitySettings settings = defaults();
            settings.timeout = timeout;
            return settings;
        }

        public static AwaitilitySettings using(Duration interval, Duration timeout) {
            AwaitilitySettings settings = defaults();
            settings.interval = interval;
            settings.timeout = timeout;
            return settings;
        }
    }
}
