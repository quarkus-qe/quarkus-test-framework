package io.quarkus.test.utils;

import io.quarkus.test.logging.Log;

public final class ProcessUtils {
    private ProcessUtils() {

    }

    public static void destroy(Process process) {
        try {
            if (process != null) {
                process.children().forEach(child -> {
                    child.destroy();
                });
                process.destroy();
                process.waitFor();
            }
        } catch (Exception e) {
            Log.warn("Error trying to stop process. Caused by " + e.getMessage());
        }
    }
}
