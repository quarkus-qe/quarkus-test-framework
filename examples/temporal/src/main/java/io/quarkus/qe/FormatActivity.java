package io.quarkus.qe;

import java.time.Duration;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

@ActivityInterface
public interface FormatActivity {

    int ACTIVITY_TIMEOUT_SEC = 10;
    int ACTIVITY_MAX_ATTEMPTS = 2;

    ActivityOptions FORMAT_ACTIVITY_DEFAULT_OPTS = ActivityOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofSeconds(ACTIVITY_TIMEOUT_SEC))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumAttempts(ACTIVITY_MAX_ATTEMPTS)
                    .build())
            .build();

    @ActivityMethod
    String composeGreeting(String name);
}
