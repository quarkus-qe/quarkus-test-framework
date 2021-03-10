package io.quarkus.test.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class FileUtils {

	private FileUtils() {

	}

	public static void deleteDirectory(Path folder) {
		File file = folder.toFile();
		if (file.exists()) {
			try {
				org.apache.commons.io.FileUtils.forceDelete(file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}