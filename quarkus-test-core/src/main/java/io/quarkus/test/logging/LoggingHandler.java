package io.quarkus.test.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.shared.utils.StringUtils;
import org.jboss.logging.Logger;

import io.quarkus.test.ServiceContext;

public abstract class LoggingHandler {

    private static final Logger LOG = Logger.getLogger(LoggingHandler.class);
    private static final long TIMEOUT_IN_MILLIS = 4000;

    private final ServiceContext context;
    private Thread innerThread;
    private List<String> logs = new ArrayList<>();
    private boolean running = false;

    protected abstract void handle();

    public LoggingHandler(ServiceContext context) {
        this.context = context;
    }

    public void startWatching() {
        logs.clear();
        running = true;
        innerThread = new Thread(this::run);
        innerThread.start();
    }

    public void stopWatching() {
        running = false;
        logs.clear();
        if (innerThread != null) {
            try {
                innerThread.interrupt();
            } catch (Exception ignored) {

            }
        }
    }

    public List<String> logs() {
        return Collections.unmodifiableList(logs);
    }

    public boolean logsContains(String expected) {
        return logs().stream().anyMatch(line -> line.contains(expected));
    }

    protected void run() {
        while (running) {
            try {
                handle();
                Thread.sleep(TIMEOUT_IN_MILLIS);
            } catch (Exception e) {
                // ignored
            }
        }
    }

    protected void onLine(String line) {
        logs.add(line);
        if (isLogEnabled()) {
            LOG.infof("[%s] %s", context.getOwner().getName(), line);
        }
    }

    protected void onLines(String lines) {
        Stream.of(lines.split("\\r?\\n")).filter(StringUtils::isNotEmpty).forEach(this::onLine);
    }

    protected void onStringDifference(String newLines, String oldLines) {
        if (StringUtils.isNotEmpty(oldLines)) {
            onLines(StringUtils.replace(newLines, oldLines, ""));
        } else {
            onLines(newLines);
        }
    }

    protected String contextName() {
        return context.getOwner().getName();
    }

    private boolean isLogEnabled() {
        return context.getOwner().getConfiguration().isTrue("log.enable");
    }

}
