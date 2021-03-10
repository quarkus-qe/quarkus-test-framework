package io.quarkus.test.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class InMemoryLoggingHandler extends LoggingHandler {

	private final List<String> logs = new LinkedList<>();

	@Override
	public boolean logsContains(String expectedOutput) {
		return logs.stream().anyMatch(line -> line.contains(expectedOutput));
	}

	@Override
	protected void handle(InputStream inputStream) {
		logs.clear();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				logs.add(line);
			}
		} catch (IOException ignored) {
		}
	}

}
