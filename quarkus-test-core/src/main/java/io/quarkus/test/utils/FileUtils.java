package io.quarkus.test.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public final class FileUtils {

    private static final int NO_RECURSIVE = 1;

    private FileUtils() {

    }

    public static void recreateDirectory(Path folder) {
        deleteDirectory(folder);
        try {
            org.apache.commons.io.FileUtils.forceMkdir(folder.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static Optional<String> findTargetFile(String endsWith) {
        return findTargetFile(StringUtils.EMPTY, endsWith);
    }

    public static Optional<String> findTargetFile(String subfolder, String endsWith) {
        try (Stream<Path> binariesFound = Files
                .find(Paths.get("target/" + subfolder), NO_RECURSIVE,
                        (path, basicFileAttributes) -> path.toFile().getName().endsWith(endsWith))) {
            return binariesFound.map(path -> path.normalize().toString()).findFirst();
        } catch (IOException ex) {
            // ignored
        }

        return Optional.empty();
    }
}
