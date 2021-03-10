package io.quarkus.test.logging;

import java.io.InputStream;

public abstract class LoggingHandler {

	private Thread innerThread;

	public abstract boolean logsContains(String expectedOutputFromSuccessfullyStarted);

	protected abstract void handle(InputStream inputStream);

	public void startWatching(InputStream inputStream) {
		innerThread = new Thread(() -> handle(inputStream));
		innerThread.start();
	}

	public void stopWatching() {
		if (innerThread != null) {
			try {
				innerThread.interrupt();
			} catch (Exception ignored) {

			}
		}
	}

}
