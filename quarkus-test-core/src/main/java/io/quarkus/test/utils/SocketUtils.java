package io.quarkus.test.utils;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;

import javax.net.ServerSocketFactory;

public final class SocketUtils {

	private static final int PORT_RANGE_MIN = 1024;
	private static final int PORT_RANGE_MAX = 65535;
	private static final SecureRandom RND = new SecureRandom();

	private SocketUtils() {

	}

	public static final int findAvailablePort() {
		int portRange = PORT_RANGE_MAX - PORT_RANGE_MIN;
		int candidatePort;
		int searchCounter = 0;
		do {
			if (searchCounter > portRange) {
				throw new IllegalStateException(
						String.format("Could not find an available port in the range [%d, %d] after %d attempts",
								PORT_RANGE_MIN, PORT_RANGE_MAX, searchCounter));
			}
			candidatePort = findRandomPort(PORT_RANGE_MIN, PORT_RANGE_MAX);
			searchCounter++;
		} while (!isPortAvailable(candidatePort));

		return candidatePort;
	}

	private static final int findRandomPort(int minPort, int maxPort) {
		int portRange = maxPort - minPort;
		return minPort + RND.nextInt(portRange + 1);
	}

	private static final boolean isPortAvailable(int port) {
		try {
			ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1,
					InetAddress.getByName("localhost"));
			serverSocket.close();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

}
