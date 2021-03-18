package io.quarkus.test.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.Service;

public class Log {
    private static final Service NO_SERVICE = null;
    private static final String COLOR_RESET = "\u001b[0m";

    private static final String COLOR_SEVERE = "\u001b[91m";
    private static final String COLOR_WARNING = "\u001b[93m";
    private static final String COLOR_DEFAULT = "\u001b[32m";

    private static final List<String> ALL_SERVICE_COLORS = Arrays.asList("\u001b[44m", // Background Blue
            "\u001b[45m", // Background Magenta
            "\u001b[46m", // Background Cyan
            "\u001b[43;1m", // Background Bright Yellow
            "\u001b[44;1m"); // Background Bright Blue
    private static final List<String> UNUSED_SERVICE_COLORS = new ArrayList<>(ALL_SERVICE_COLORS);

    private static final Random RND = new Random();
    private static final Logger LOG = Logger.getLogger(Log.class.getName());

    private static final Map<String, String> SERVICE_COLOR_MAPPING = new HashMap<>();

    public static void info(Service service, String msg, Object... args) {
        log(service, Level.INFO, msg, args);
    }

    public static void info(String msg, Object... args) {
        log(NO_SERVICE, Level.INFO, msg, args);
    }

    public static void debug(Service service, String msg, Object... args) {
        log(service, Level.FINE, msg, args);
    }

    public static void warn(Service service, String msg, Object... args) {
        log(service, Level.WARNING, msg, args);
    }

    public static void warn(String msg, Object... args) {
        log(NO_SERVICE, Level.WARNING, msg, args);
    }

    private static final void log(Service service, Level level, String msg, Object... args) {
        String prefix = findColorForService(service);
        if (level == Level.SEVERE) {
            prefix = COLOR_SEVERE;
        } else if (level == Level.WARNING) {
            prefix = COLOR_WARNING;
        }

        LOG.log(level, prefix + inBrackets(service) + String.format(msg, args) + COLOR_RESET);
    }

    private static synchronized String findColorForService(Service service) {
        if (service == null) {
            return COLOR_DEFAULT;
        }

        String color = SERVICE_COLOR_MAPPING.get(service.getName());
        if (color == null) {
            if (UNUSED_SERVICE_COLORS.isEmpty()) {
                // reset if no more available service colors
                UNUSED_SERVICE_COLORS.addAll(ALL_SERVICE_COLORS);
            }

            color = UNUSED_SERVICE_COLORS.remove(RND.nextInt(UNUSED_SERVICE_COLORS.size() - 1));
            SERVICE_COLOR_MAPPING.put(service.getName(), color);
        }

        return color;
    }

    private static final String inBrackets(Service service) {
        if (service == null) {
            return StringUtils.EMPTY;
        }

        return String.format("[%s] ", service.getName());
    }

}
