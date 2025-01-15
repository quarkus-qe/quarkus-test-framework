package io.quarkus.test.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

public final class FileUtils {

    private static final int NO_RECURSIVE = 1;

    private FileUtils() {

    }

    public static Path copyContentTo(String content, Path target) {
        try {
            Files.writeString(target, content);
        } catch (IOException e) {
            fail("Failed when writing file " + target, e);
        }

        return target;
    }

    public static String loadFile(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not load file " + file, e);
        }

        return null;
    }

    public static String loadFile(String file) {
        try {
            return IOUtils.toString(
                    FileUtils.class.getResourceAsStream(file),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not load file " + file, e);
        }

        return EMPTY;
    }

    public static void recreateDirectory(Path folder) {
        deletePath(folder);
        createDirectory(folder);
    }

    public static void createDirectory(Path folder) {
        try {
            org.apache.commons.io.FileUtils.forceMkdir(folder.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createDirectoryIfDoesNotExist(Path folder) {
        if (!Files.exists(folder)) {
            folder.toFile().mkdirs();
        }
    }

    public static void copyFileTo(File file, Path target) {
        try {
            org.apache.commons.io.FileUtils.copyFileToDirectory(file, target.toFile());
        } catch (IOException e) {
            fail("Could not copy project.", e);
        }
    }

    public static void copyFileTo(String resourceFileName, Path targetPath) {
        try (InputStream resources = FileUtils.class.getClassLoader().getResourceAsStream(resourceFileName)) {
            Files.copy(resources, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void copyDirectoryTo(Path source, Path target) {
        try {
            org.apache.commons.io.FileUtils.copyDirectory(source.toFile(), target.toFile());
        } catch (IOException e) {
            fail("Could not copy project.", e);
        }
    }

    public static void copyCurrentDirectoryTo(Path target) {
        copyDirectoryTo(Paths.get("."), target);
    }

    public static void deletePath(Path folder) {
        deleteFile(folder.toFile());
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            try {
                org.apache.commons.io.FileUtils.forceDelete(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void deleteFileContent(File file) {
        if (file.exists()) {
            try {
                org.apache.commons.io.FileUtils.write(file, "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Optional<String> findTargetFile(Path basePath, String endsWith) {
        return findTargetFile(basePath, fileName -> fileName.endsWith(endsWith));
    }

    public static Optional<String> findTargetFile(Path basePath, Predicate<String> fileNameMatcher) {
        try (Stream<Path> binariesFound = Files
                .find(basePath, NO_RECURSIVE,
                        (path, basicFileAttributes) -> fileNameMatcher.test(path.toFile().getName()))) {
            return binariesFound.map(path -> path.normalize().toString()).findFirst();
        } catch (IOException ex) {
            // ignored
        }

        return Optional.empty();
    }

}
