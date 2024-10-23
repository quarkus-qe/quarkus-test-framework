package io.quarkus.test.plugin.preparer;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

final class PreparerTestUtils {

    static final boolean SKIP_INTEGRATION_TESTS = Boolean.getBoolean("skipITs");
    private static final String TARGET_POM = "quarkus-app-pom.xml";

    private PreparerTestUtils() {
        // UTILS
    }

    static boolean targetPomExists(MavenProject project) {
        return Files.exists(getTargetPomPath(project));
    }

    static Path getTargetPomPath(MavenProject project) {
        return getCurrentProjectTarget(project).resolve(TARGET_POM);
    }

    static Path getCurrentProjectTarget(MavenProject project) {
        var targetPath = project.getBasedir().toPath().resolve("target");
        if (!Files.exists(targetPath)) {
            targetPath.toFile().mkdirs();
        }
        return targetPath;
    }
}
