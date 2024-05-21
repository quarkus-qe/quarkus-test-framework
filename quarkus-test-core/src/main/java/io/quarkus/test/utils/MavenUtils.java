package io.quarkus.test.utils;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.PropertyLookup;

public final class MavenUtils {

    public static final String MVN_COMMAND = "mvn";
    public static final String PACKAGE_GOAL = "package";
    public static final String INSTALL_GOAL = "install";
    public static final String MVN_REPOSITORY_LOCAL = "maven.repo.local";
    public static final String ENSURE_QUARKUS_BUILD = "-Dquarkus.build.skip=false";
    public static final String SKIP_TESTS = "-DskipTests=true";
    public static final String SKIP_ITS = "-DskipITs=true";
    public static final String DISPLAY_ERRORS = "-e";
    public static final String BATCH_MODE = "-B";
    public static final String DISPLAY_VERSION = "-V";
    public static final String SKIP_CHECKSTYLE = "-Dcheckstyle.skip";
    public static final String QUARKUS_PROFILE = "quarkus.profile";
    public static final String QUARKUS_PROPERTY_PREFIX = "quarkus";
    public static final String POM_XML = "pom.xml";

    private static final PropertyLookup PROPAGATE_PROPERTIES_STRATEGY = new PropertyLookup(
            "maven.propagate-properties-strategy", PropagatePropertiesStrategy.ALL.getValue());
    private static final PropertyLookup PROPAGATE_PROPERTIES_STRATEGY_ALL_EXCLUSIONS = new PropertyLookup(
            "maven.propagate-properties-strategy.all.exclude");

    private MavenUtils() {

    }

    public static void build(ServiceContext serviceContext, List<String> extraMavenArgs) {
        build(serviceContext, serviceContext.getServiceFolder(), extraMavenArgs);
    }

    public static void build(ServiceContext serviceContext, Path basePath, List<String> extraMavenArgs) {
        List<String> command = mvnCommand(serviceContext);
        command.addAll(extraMavenArgs);
        command.add(DISPLAY_ERRORS);
        command.add(DISPLAY_VERSION);
        command.add(PACKAGE_GOAL);
        try {
            new Command(command)
                    .outputToConsole()
                    .onDirectory(basePath)
                    .runAndWait();

        } catch (Exception e) {
            fail("Failed to build Maven. Caused by: " + e.getMessage());
        }
    }

    public static List<String> devModeMavenCommand(ServiceContext serviceContext, List<String> systemProperties) {
        List<String> command = mvnCommand(serviceContext);
        command.addAll(Arrays.asList(SKIP_CHECKSTYLE, SKIP_ITS));
        command.addAll(systemProperties);
        command.add(withProperty("debug", "false"));
        command.add("quarkus:dev");

        return command;
    }

    public static List<String> mvnCommand(ServiceContext serviceContext) {
        List<String> args = new ArrayList<>();
        args.add(MVN_COMMAND);
        args.add(DISPLAY_ERRORS);
        args.add(withQuarkusProfile(serviceContext));
        withMavenRepositoryLocalIfSet(args);
        withProperties(args);
        return args;
    }

    public static String withProperty(String property, String value) {
        return String.format("-D%s=%s", property, value);
    }

    public static void installParentPomsIfNeeded() {
        installParentPomsIfNeeded(Paths.get("."));
    }

    public static void installParentPomsIfNeeded(Path basePath) {
        if (Files.exists(basePath.resolve(POM_XML))) {
            Model mavenModel = getMavenModel(basePath);
            if (mavenModel != null && mavenModel.getParent() != null) {
                Path relativePath = basePath
                        .resolve(StringUtils.defaultIfBlank(mavenModel.getParent().getRelativePath(), ".."));
                installParentPom(relativePath);
                installParentPomsIfNeeded(relativePath);
            }
        }
    }

    private static void installParentPom(Path relativePath) {
        List<String> args = new ArrayList<>();
        args.addAll(asList(MVN_COMMAND, DISPLAY_ERRORS, INSTALL_GOAL, SKIP_CHECKSTYLE, SKIP_TESTS, SKIP_ITS, "-pl", "."));
        withMavenRepositoryLocalIfSet(args);
        withProperties(args);

        Command cmd = new Command(args);
        cmd.onDirectory(relativePath);
        try {
            cmd.runAndWait();
        } catch (Exception ignored) {
            // Could not find POM.xml, ignoring
        }
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

    private static Model getMavenModel(Path basePath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileReader is = new FileReader(basePath.resolve(POM_XML).toFile())) {
            return reader.read(is);
        } catch (Exception ignored) {
            // Could not find POM.xml, ignoring
        }

        return null;
    }

    private static void withProperties(List<String> args) {

        PropagatePropertiesStrategy strategy = PropagatePropertiesStrategy.fromValue(PROPAGATE_PROPERTIES_STRATEGY.get());

        System.getProperties().entrySet().stream()
                .filter(strategy.isPropagated().and(propertyValueIsNotEmpty()))
                .forEach(property -> {
                    String key = (String) property.getKey();
                    String value = (String) property.getValue();
                    args.add(withProperty(key, value));
                });
    }

    private static Predicate<Map.Entry<Object, Object>> propertyValueIsNotEmpty() {
        return property -> StringUtils.isNotEmpty((String) property.getValue());
    }

    private enum PropagatePropertiesStrategy {
        ALL("all", name -> PROPAGATE_PROPERTIES_STRATEGY_ALL_EXCLUSIONS.getAsList()
                .stream().noneMatch(name::startsWith)),
        NONE("none", name -> false),
        ONLY_QUARKUS("only-quarkus", name -> StringUtils.startsWith(name, QUARKUS_PROPERTY_PREFIX));

        private final String value;
        private final Predicate<String> predicate;

        PropagatePropertiesStrategy(String value, Predicate<String> predicate) {
            this.value = value;
            this.predicate = predicate;
        }

        public String getValue() {
            return value;
        }

        public Predicate<Map.Entry<Object, Object>> isPropagated() {
            return property -> predicate.test((String) property.getKey());
        }

        public static PropagatePropertiesStrategy fromValue(String value) {
            for (PropagatePropertiesStrategy strategy : PropagatePropertiesStrategy.values()) {
                if (strategy.getValue().equalsIgnoreCase(value)) {
                    return strategy;
                }
            }

            throw new IllegalArgumentException("Property `maven.propagate-properties-strategy` with invalid value: " + value);
        }
    }
}
