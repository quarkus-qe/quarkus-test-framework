package io.quarkus.test.logging;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.apache.maven.shared.utils.StringUtils;

import io.quarkus.test.utils.AwaitilityUtils;

public abstract class LoggingHandler implements Closeable {

    private static final long TIMEOUT_IN_MILLIS = 4000;
    private static final String ANY = ".*";

    private Thread innerThread;
    private List<String> logs = new CopyOnWriteArrayList<>();
    private boolean running = false;

    protected abstract void handle();

    public void startWatching() {
        logs.clear();
        running = true;
        innerThread = new Thread(this::run);
        innerThread.setDaemon(true);
        innerThread.start();
    }

    public void stopWatching() {
        flush();
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
        return logs().stream().anyMatch(line -> line.matches(ANY + expected + ANY));
    }

    public void flush() {
        AwaitilityUtils.untilAsserted(this::handle);
    }

    @Override
    public void close() {
        if (running) {
            stopWatching();
        }
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
            logInfo(line);
        }
    }

    protected void logInfo(String line) {
        Log.info(line);
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

    protected boolean isLogEnabled() {
        return true;
    }

}
