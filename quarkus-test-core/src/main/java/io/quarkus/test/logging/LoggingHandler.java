package io.quarkus.test.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.test.ServiceContext;

public abstract class LoggingHandler {

    private static final Logger LOG = Logger.getLogger(LoggingHandler.class);

    private final ServiceContext context;
    private Thread innerThread;
    private List<String> logs = new ArrayList<>();

    protected abstract void handle();

    public LoggingHandler(ServiceContext context) {
        this.context = context;
    }

    public void startWatching() {
        logs.clear();
        innerThread = new Thread(this::handle);
        innerThread.start();
    }

    public void stopWatching() {
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

    protected void onLine(String line) {
        logs.add(line);
        if (isLogEnabled()) {
            LOG.infof("[%s] %s", context.getOwner().getName(), line);
        }
    }

    private boolean isLogEnabled() {
        return context.getOwner().getConfiguration().isTrue("log.enable");
    }

}
