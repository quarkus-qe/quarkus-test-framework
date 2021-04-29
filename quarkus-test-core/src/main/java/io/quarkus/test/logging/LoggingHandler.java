package io.quarkus.test.logging;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.apache.maven.shared.utils.StringUtils;

import io.quarkus.test.bootstrap.ServiceContext;

public abstract class LoggingHandler {

    private static final long TIMEOUT_IN_MILLIS = 4000;

    private final ServiceContext context;
    private Thread innerThread;
    private List<String> logs = new CopyOnWriteArrayList<>();
    private boolean running = false;

    public LoggingHandler(ServiceContext context) {
        this.context = context;
    }

    protected abstract void handle();

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
            } catch (Exception ignored) {

            }
        }
    }

    protected void onLine(String line) {
        logs.add(line);
        if (isLogEnabled()) {
            Log.info(context.getOwner(), line);
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
        return context.getName();
    }

    private boolean isLogEnabled() {
        return context.getOwner().getConfiguration().isTrue("log.enable");
    }

}
