package io.quarkus.test.logging;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.FileHandler;

import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.test.bootstrap.QuarkusScenarioBootstrap;
import io.quarkus.test.bootstrap.ScenarioContext;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.configuration.PropertyLookup;

public final class Log {
    public static final PropertyLookup LOG_LEVEL = new PropertyLookup("log.level");
    public static final PropertyLookup LOG_FORMAT = new PropertyLookup("log.format");
    public static final PropertyLookup LOG_FILE_OUTPUT = new PropertyLookup("log.file.output");
    public static final PropertyLookup LOG_NOCOLOR = new PropertyLookup("log.nocolor", "false");

    public static final String LOG_SUFFIX = ".log";

    private static final Service NO_SERVICE = null;
    private static final String COLOR_RESET = "\u001b[0m";

    private static final String COLOR_SEVERE = "\u001b[91m";
    private static final String COLOR_WARNING = "\u001b[93m";
    private static final String COLOR_DEFAULT = "\u001b[32m";

    private static final boolean NOCOLOR = LOG_NOCOLOR.getAsBoolean();
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

    public static void error(Service service, String msg, Object... args) {
        log(service, Level.SEVERE, msg, args);
    }

    public static void error(String msg, Object... args) {
        log(NO_SERVICE, Level.SEVERE, msg, args);
    }

    public static void configure(ScenarioContext scenario) {
        // Configure Log Manager
        try (InputStream in = QuarkusScenarioBootstrap.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            // ignore
        }

        String logPattern = LOG_FORMAT.get();
        Level level = Level.parse(LOG_LEVEL.get());

        // Configure logger handlers
        Logger logger = LogManager.getLogManager().getLogger("");
        logger.setLevel(level);

        // Remove existing handlers
        for (Handler handler : logger.getHandlers()) {
            // JBosss context is saved statically and when more tests are run inside module
            // while org.jboss.logmanager.LogManager is installed we add a new handlers in addition to previous ones
            // it's desirable to install only a new handlers according to test configuration
            // QuarkusDelayedHandler is removed as it duplicates logs when JBoss log manager is installed
            if (handler instanceof QuarkusDelayedHandler || handler instanceof ConsoleHandler
                    || handler instanceof FileHandler) {
                logger.removeHandler(handler);
            }
        }

        // - Console
        ConsoleHandler console = new ConsoleHandler(
                ConsoleHandler.Target.SYSTEM_OUT,
                NOCOLOR ? new PatternFormatter(logPattern) : new ColorPatternFormatter(logPattern));
        console.setLevel(level);
        logger.addHandler(console);

        // - File
        try {
            FileHandler file = new FileHandler(
                    new PatternFormatter(logPattern),
                    scenario.getLogFile().toFile());
            file.setLevel(level);
            logger.addHandler(file);
        } catch (Exception ex) {
            Log.warn("Could not configure file handler. Caused by " + ex);
        }
    }

    private static void log(Service service, Level level, String msg, Object... args) {
        if (isServiceLogLevelAllowed(service, level)) {
            String textColor = findColorForText(level, service);
            String logMessage = msg;
            if (args != null && args.length > 0) {
                logMessage = String.format(msg, args);
            }
            if (NOCOLOR) {
                LOG.log(level, inBrackets(service) + logMessage);
            } else {
                LOG.log(level, textColor + inBrackets(service) + logMessage + COLOR_RESET);
            }
        }
    }

    private static boolean isServiceLogLevelAllowed(Service service, Level level) {
        boolean enabled = true;
        if (Objects.nonNull(service) && Objects.nonNull(service.getConfiguration())) {
            String serviceLogLevel = service.getConfiguration().getOrDefault(Configuration.Property.LOG_LEVEL_NAME, EMPTY);
            if (!serviceLogLevel.isEmpty()) {
                enabled = Level.parse(serviceLogLevel).intValue() <= level.intValue();
            }
        }
        return enabled;
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
            return EMPTY;
        }

        return String.format("[%s] ", service.getName());
    }

}
