package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.logging.Log;

public final class ClassPathUtils {
    private static final Path SOURCE_CLASSES_LOCATION = Paths.get("target", "classes");
    private static final String CLASS_SUFFIX = ".class";

    private ClassPathUtils() {

    }

    public static Class<?>[] findAllClassesFromSource() {
        List<Class<?>> classes = new LinkedList<>();
        try {
            if (!Files.exists(SOURCE_CLASSES_LOCATION)) {
                return new Class<?>[0];
            }
            try (Stream<Path> stream = Files.walk(SOURCE_CLASSES_LOCATION)) {
                stream.map(Path::toString)
                        .filter(s -> s.endsWith(CLASS_SUFFIX))
                        .map(ClassPathUtils::normalizeClassName)
                        .forEach(className -> {
                            try {
                                classes.add(Thread.currentThread().getContextClassLoader().loadClass(className));
                            } catch (ClassNotFoundException ex) {
                                Log.warn("Could not load %s. Caused by: %s", className, ex);
                            }
                        });
            }
        } catch (Exception ex) {
            fail("Can't load source classes location. Caused by " + ex.getMessage());
        }

        return classes.toArray(new Class<?>[classes.size()]);
    }

    private static String normalizeClassName(String path) {
        return normalizeClassName(path, CLASS_SUFFIX);
    }

    public static String normalizeClassName(String path, String suffix) {
        String source = SOURCE_CLASSES_LOCATION.relativize(Paths.get(path)).toString()
                .replace(suffix, StringUtils.EMPTY);
        if (OS.WINDOWS.isCurrentOs()) {
            source = source.replace("\\", ".");
        } else {
            source = source.replace("/", ".");
        }

        return source;
    }
}
