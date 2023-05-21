package io.quarkus.test.utils;

import static io.quarkus.test.utils.AwaitilityUtils.untilIsFalse;
import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings.usingTimeout;
import static java.time.Duration.ofMinutes;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.logging.Log;

public final class ProcessUtils {

    private static final int PROCESS_KILL_TIMEOUT_MINUTES = 3;

    private ProcessUtils() {

    }

    public static void destroy(Process process) {
        try {
            if (process != null) {
                process.descendants().forEach(child -> {
                    if (child.supportsNormalTermination()) {
                        child.destroy();
                        untilIsFalse(process::isAlive, usingTimeout(ofMinutes(PROCESS_KILL_TIMEOUT_MINUTES)));
                    }

                    if (child.isAlive()) {
                        child.destroyForcibly();
                    }

                    pidKiller(child.pid());

                    untilIsFalse(child::isAlive);
                });

                if (process.supportsNormalTermination()) {
                    process.destroy();
                    process.waitFor(PROCESS_KILL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                }

                pidKiller(process.pid());
                untilIsFalse(process::isAlive);
            }
        } catch (Exception e) {
            Log.warn("Error trying to stop process. Caused by " + e.getMessage());
        }
    }

    private static void pidKiller(long pid) {
        try {
            if (OS.WINDOWS.isCurrentOs()) {
                Runtime.getRuntime().exec(new String[] { "cmd", "/C", "taskkill", "/PID", Long.toString(pid), "/F", "/T" });
            } else {
                Runtime.getRuntime().exec(new String[] { "kill", "-9", Long.toString(pid) });
            }
        } catch (Exception e) {
            Log.warn("Error stopping process " + pid, e);
        }
    }
}
