package io.quarkus.test.logging;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.Service;

public final class Log {
    public static final Path LOG_OUTPUT_DIRECTORY = Path.of("target", "logs");
    public static final String LOG_SUFFIX = ".log";

    private static final Service NO_SERVICE = null;
    private static final String COLOR_RESET = "\u001b[0m";

    private static final String COLOR_SEVERE = "\u001b[91m";
    private static final String COLOR_WARNING = "\u001b[93m";
    private static final String COLOR_DEFAULT = "\u001b[32m";

    private static final List<String> ALL_SERVICE_COLORS = Arrays.asList("\u001b[0;34m", // Blue
            "\u001b[0;95m", // Magenta
            "\u001b[0;96m", // Cyan
            "\u001b[0;93m", // Bright Yellow
            "\u001b[0;94m"); // Bright Blue
    private static final List<String> UNUSED_SERVICE_COLORS = new ArrayList<>(ALL_SERVICE_COLORS);

    private static final Random RND = new Random();
    private static final Logger LOG = Logger.getLogger(Log.class.getName());

    private static final Map<String, String> SERVICE_COLOR_MAPPING = new HashMap<>();

    private Log() {

    }

    public static void info(Service service, String msg, Object... args) {
        log(service, Level.INFO, msg, args);
    }

    public static void info(String msg, Object... args) {
        log(NO_SERVICE, Level.INFO, msg, args);
    }

    public static void debug(Service service, String msg, Object... args) {
        log(service, Level.FINE, msg, args);
    }

    public static void debug(String msg, Object... args) {
        log(NO_SERVICE, Level.FINE, msg, args);
    }

    public static void warn(Service service, String msg, Object... args) {
        log(service, Level.WARNING, msg, args);
    }

    public static void warn(String msg, Object... args) {
        log(NO_SERVICE, Level.WARNING, msg, args);
    }

    private static void log(Service service, Level level, String msg, Object... args) {
        String textColor = findColorForText(level, service);
        String logMessage = msg;
        if (args != null && args.length > 0) {
            logMessage = String.format(msg, args);
        }

        LOG.log(level, textColor + inBrackets(service) + logMessage + COLOR_RESET);
    }

    private static synchronized String findColorForText(Level level, Service service) {
        String textColor = findColorForService(service);
        if (level == Level.SEVERE) {
            textColor = COLOR_SEVERE;
        } else if (level == Level.WARNING) {
            textColor = COLOR_WARNING;
        }

        return textColor;
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

            int colorIdx = 0;
            if (UNUSED_SERVICE_COLORS.size() > 1) {
                colorIdx = RND.nextInt(UNUSED_SERVICE_COLORS.size() - 1);
            }

            color = UNUSED_SERVICE_COLORS.remove(colorIdx);
            SERVICE_COLOR_MAPPING.put(service.getName(), color);
        }

        return color;
    }

    private static String inBrackets(Service service) {
        if (service == null) {
            return StringUtils.EMPTY;
        }

        return String.format("[%s] ", service.getName());
    }

}
