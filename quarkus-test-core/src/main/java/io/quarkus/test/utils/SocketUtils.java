package io.quarkus.test.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.test.configuration.PropertyLookup;

public final class SocketUtils {

    private static final PropertyLookup PORT_RANGE_MIN_PROPERTY = new PropertyLookup("port.range.min", "1100");
    private static final PropertyLookup PORT_RANGE_MAX_PROPERTY = new PropertyLookup("port.range.max", "49151");
    private static final PropertyLookup PORT_RESOLUTION_STRATEGY_PROPERTY = new PropertyLookup("port.resolution.strategy");

    private static final int PORT_RANGE_MIN = PORT_RANGE_MIN_PROPERTY.getAsInteger();
    private static final int PORT_RANGE_MAX = PORT_RANGE_MAX_PROPERTY.getAsInteger();
    private static final String PORT_RESOLUTION_RANDOM_STRATEGY = "random";

    private static final AtomicInteger CURRENT_MIN_PORT = new AtomicInteger(PORT_RANGE_MIN);
    private static final Random RND = new Random(System.nanoTime());

    private SocketUtils() {

    }

    public static synchronized int findAvailablePort() {
        if (PORT_RESOLUTION_RANDOM_STRATEGY.equals(PORT_RESOLUTION_STRATEGY_PROPERTY.get())) {
            return findRandomAvailablePort();
        }

        return findNextAvailablePort();
    }

    public static int findRandomAvailablePort() {
        int portRange = PORT_RANGE_MAX - PORT_RANGE_MIN;
        int candidatePort;
        int searchCounter = 0;
        do {
            if (searchCounter > portRange) {
                throw new IllegalStateException(
                        String.format("Could not find an available port in the range [%d, %d] after %d attempts",
                                PORT_RANGE_MIN, PORT_RANGE_MAX, searchCounter));
            }

            candidatePort = PORT_RANGE_MIN + RND.nextInt((PORT_RANGE_MAX - PORT_RANGE_MIN) + 1);
            searchCounter++;
        } while (!isPortAvailable(candidatePort));

        return candidatePort;
    }

    public static synchronized int findNextAvailablePort() {
        int candidate;
        do {
            candidate = CURRENT_MIN_PORT.incrementAndGet();
            if (isPortAvailable(candidate)) {
                return candidate;
            }
        } while (candidate <= PORT_RANGE_MAX);

        throw new IllegalStateException(String.format("Could not find an available port in the range [%d, %d]",
                PORT_RANGE_MIN, PORT_RANGE_MAX));
    }

    private static boolean isPortAvailable(int port) {
        if (port < PORT_RANGE_MIN || port > PORT_RANGE_MAX) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
            // do nothing: port not available
        }

        return false;
    }

}
