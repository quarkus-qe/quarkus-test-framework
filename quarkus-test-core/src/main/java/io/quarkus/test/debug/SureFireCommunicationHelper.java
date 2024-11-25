package io.quarkus.test.debug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SureFireCommunicationHelper {

    private static final String EXIT_PREFIX = "quarkus-fw-debug-mode-exit-";
    private final Path exitTmpDir;

    private SureFireCommunicationHelper(boolean performLookup) {
        if (performLookup) {
            // temporary directory was created by master process and this is fork, we need exactly one directory
            // to check for exit files
            exitTmpDir = findExitTmpDir();
        } else {
            deletePreviousExitTmpDirs();
            exitTmpDir = createExitTemporaryDirectory();
        }
    }

    void closeCommunication() {
        deleteDirectory(exitTmpDir);
    }

    void sendExitSignal() {
        try {
            File.createTempFile(EXIT_PREFIX, null, exitTmpDir.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create exit file", e);
        }
    }

    boolean receivedExitSignal() {
        try (var tmpFiles = Files.list(exitTmpDir)) {
            return tmpFiles.findAny().isPresent();
        } catch (IOException e) {
            throw new RuntimeException("Failed to detect exit file", e);
        }
    }

    static SureFireCommunicationHelper startReceiverCommunication() {
        return new SureFireCommunicationHelper(true);
    }

    static SureFireCommunicationHelper startSenderCommunication() {
        return new SureFireCommunicationHelper(false);
    }

    private static Path findExitTmpDir() {
        try (var tmpFiles = Files.list(tmpDirPath())) {
            final Path exitTmpDir;
            var exitTmpDirs = tmpFiles
                    .filter(Files::isDirectory)
                    .filter(SureFireCommunicationHelper::isExitTmpDir)
                    .toList();
            if (exitTmpDirs.isEmpty()) {
                throw new IllegalStateException("Exit directory is missing");
            }
            if (exitTmpDirs.size() > 1) {
                throw new IllegalStateException("Previous exit directories are present");
            }
            exitTmpDir = exitTmpDirs.get(0);
            return exitTmpDir;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to find exit directory", e);
        }
    }

    private static boolean isExitTmpDir(Path path) {
        return path.getFileName().toString().startsWith(EXIT_PREFIX);
    }

    private static Path createExitTemporaryDirectory() {
        try {
            return Files.createTempDirectory(EXIT_PREFIX);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deletePreviousExitTmpDirs() {
        var tmpDir = tmpDirPath();
        try (var tmpFiles = Files.list(tmpDir)) {
            tmpFiles
                    .filter(Files::isDirectory)
                    .filter(SureFireCommunicationHelper::isExitTmpDir)
                    .forEach(SureFireCommunicationHelper::deleteDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete previous exit directories", e);
        }
    }

    private static Path tmpDirPath() {
        return Path.of(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Recursively deletes directory with all its content.
     */
    private static void deleteDirectory(Path directoryPath) {
        if (Files.exists(directoryPath)) {
            var dirFiles = directoryPath.toFile().listFiles();
            if (dirFiles != null) {
                for (File dirFile : dirFiles) {
                    if (dirFile.isDirectory()) {
                        deleteDirectory(dirFile.toPath());
                    } else {
                        deletePath(dirFile.toPath());
                    }
                }
            }
            deletePath(directoryPath);
        }
    }

    private static void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete: " + path, e);
        }
    }
}
