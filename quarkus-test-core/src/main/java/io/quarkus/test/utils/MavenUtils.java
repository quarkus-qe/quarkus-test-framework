package io.quarkus.test.utils;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.bootstrap.ServiceContext;

public final class MavenUtils {

    public static final String MVN_COMMAND = "mvn";
    public static final String PACKAGE_GOAL = "package";
    public static final String MVN_REPOSITORY_LOCAL = "maven.repo.local";
    public static final String SKIP_TESTS = "-DskipTests=true";
    public static final String SKIP_ITS = "-DskipITs=true";
    public static final String BATCH_MODE = "-B";
    public static final String DISPLAY_VERSION = "-V";
    public static final String SKIP_CHECKSTYLE = "-Dcheckstyle.skip";
    public static final String QUARKUS_PROFILE = "quarkus.profile";

    private MavenUtils() {

    }

    public static List<String> mvnCommand(ServiceContext serviceContext) {
        List<String> args = new ArrayList<>();
        args.add(MVN_COMMAND);
        args.add(withQuarkusProfile(serviceContext));
        withMavenRepositoryLocalIfSet(args);
        return args;
    }

    private static String withQuarkusProfile(ServiceContext serviceContext) {
        return withProperty(QUARKUS_PROFILE, serviceContext.getTestContext().getRequiredTestClass().getSimpleName());
    }

    private static void withMavenRepositoryLocalIfSet(List<String> args) {
        String mvnRepositoryPath = System.getProperty(MVN_REPOSITORY_LOCAL);
        if (mvnRepositoryPath != null) {
            args.add(withProperty(MVN_REPOSITORY_LOCAL, mvnRepositoryPath));
        }
    }

    public static String withProperty(String property, String value) {
        return String.format("-D%s=%s", property, value);
    }
}
