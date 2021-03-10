package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

public final class ClassPathUtils {

	private static final Logger LOGGER = Logger.getLogger(ClassPathUtils.class);
	private static final String SOURCE_CLASSES_LOCATION = "target/classes/";
	private static final String CLASS_SUFFIX = ".class";

	private ClassPathUtils() {

	}

	public static final Class<?>[] findAllClassesFromSource() {
		List<Class<?>> classes = new LinkedList<>();
		try {
			Path classesPathInSources = Path.of(SOURCE_CLASSES_LOCATION);
			try (Stream<Path> stream = Files.walk(classesPathInSources)) {
				stream.map(Path::toString).filter(s -> s.endsWith(CLASS_SUFFIX)).map(ClassPathUtils::normalizeClassName)
						.forEach(className -> {
							try {
								classes.add(Thread.currentThread().getContextClassLoader().loadClass(className));
							} catch (ClassNotFoundException ex) {
								LOGGER.warnf("Could not load %s. Caused by: %s", className, ex);
							}
						});
			}
		} catch (Exception ex) {
			fail("Can't load source classes location. Caused by " + ex.getMessage());
		}

		return classes.toArray(new Class<?>[classes.size()]);
	}

	private static final String normalizeClassName(String path) {
		return path.replace(CLASS_SUFFIX, "").replace(SOURCE_CLASSES_LOCATION, "").replace("/", ".");
	}
}
