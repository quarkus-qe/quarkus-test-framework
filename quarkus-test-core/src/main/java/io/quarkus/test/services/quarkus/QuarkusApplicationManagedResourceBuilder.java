package io.quarkus.test.services.quarkus;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.MapUtils;
import io.quarkus.test.utils.PropertiesUtils;

public abstract class QuarkusApplicationManagedResourceBuilder implements ManagedResourceBuilder {

    private static final String BUILD_TIME_PROPERTIES = "/build-time-list";
    private static final String RESOURCES_FOLDER = "src/main/resources";
    private static final String TEST_RESOURCES_FOLDER = "src/test/resources";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final List<String> RESOURCES_TO_COPY = Arrays.asList(".sql", ".keystore", ".truststore");
    private static final Set<String> BUILD_PROPERTIES = FileUtils.loadFile(BUILD_TIME_PROPERTIES).lines().collect(toSet());

    private Class<?>[] appClasses;
    private boolean selectedAppClasses = true;
    private ServiceContext context;
    private boolean sslEnabled = false;
    private Map<String, String> propertiesSnapshot;

    protected abstract void build();

    protected ServiceContext getContext() {
        return context;
    }

    protected void setContext(ServiceContext context) {
        this.context = context;
    }

    protected boolean isSslEnabled() {
        return sslEnabled;
    }

    protected void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    protected Class<?>[] getAppClasses() {
        return appClasses;
    }

    protected boolean isSelectedAppClasses() {
        return selectedAppClasses;
    }

    public boolean containsBuildProperties() {
        Map<String, String> differenceProperties = MapUtils.difference(context.getOwner().getProperties(), propertiesSnapshot);
        Set<String> properties = differenceProperties.keySet();
        if (properties.isEmpty()) {
            return false;
        }

        return properties.stream().anyMatch(this::isBuildProperty);
    }

    public Map<String, String> createSnapshotOfBuildProperties() {
        propertiesSnapshot = new HashMap<>(context.getOwner().getProperties());
        return new HashMap<>(context.getOwner().getProperties());
    }

    public Map<String, String> getBuildProperties() {
        return getContext().getOwner().getProperties().entrySet().stream()
                .filter(e -> isBuildProperty(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected void initAppClasses(Class<?>[] classes) {
        appClasses = classes;
        if (appClasses.length == 0) {
            appClasses = ClassPathUtils.findAllClassesFromSource();
            selectedAppClasses = false;
        }
    }

    protected void configureLogging() {
        context.getOwner().withProperty("quarkus.log.console.format", "%d{HH:mm:ss,SSS} %s%e%n");
    }

    protected void copyResourcesToAppFolder() {
        copyResourcesInFolderToAppFolder(RESOURCES_FOLDER);
        copyResourcesInFolderToAppFolder(TEST_RESOURCES_FOLDER);
        PropertiesUtils.fromMap(createSnapshotOfBuildProperties(),
                context.getServiceFolder().resolve(APPLICATION_PROPERTIES));
    }

    private boolean isBuildProperty(String name) {
        return BUILD_PROPERTIES.stream().anyMatch(build -> name.matches(build) || name.startsWith(build));
    }

    private void copyResourcesInFolderToAppFolder(String folder) {
        try (Stream<Path> binariesFound = Files
                .find(Paths.get(folder), Integer.MAX_VALUE,
                        (path, basicFileAttributes) -> RESOURCES_TO_COPY.stream()
                                .anyMatch(filter -> path.toFile().getName().endsWith(filter)))) {
            binariesFound.forEach(path -> FileUtils.copyFileTo(path.toFile(), context.getServiceFolder()));
        } catch (IOException ex) {
            // ignored
        }
    }

}
