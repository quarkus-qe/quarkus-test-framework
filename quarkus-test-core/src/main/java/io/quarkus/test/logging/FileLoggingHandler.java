package io.quarkus.test.logging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import io.quarkus.bootstrap.util.IoUtils;

public class FileLoggingHandler extends LoggingHandler {

	private final Path file;

	public FileLoggingHandler(Path file) {
		this.file = file;
	}

	@Override
	public boolean logsContains(String expectedOutput) {
		try {
			return IoUtils.readFile(file).contains(expectedOutput);
		} catch (IOException ignored) {
			return false;
		}
	}

	@Override
	protected void handle(InputStream inputStream) {
		try (OutputStream outStream = new FileOutputStream(file.toFile())) {
			byte[] buffer = new byte[8 * 1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException ignored) {
		}
	}

}
