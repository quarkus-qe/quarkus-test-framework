package io.quarkus.test.plugin.preparer;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

// if you ever get checkstyle line length warning for the @Mojo, adjust checkstyle-suppressions.xml
@Mojo(name = "prepare-pom-mojo", defaultPhase = PACKAGE, requiresDependencyCollection = COMPILE, requiresDependencyResolution = COMPILE, threadSafe = true)
public class PreparePomMojo extends AbstractMojo {

    private static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";
    private static final boolean SKIP_INTEGRATION_TESTS = Boolean.getBoolean("skipITs");
    // this needs to be system property, it is too early for FW configuration
    private static final Set<String> PROPAGATED_ANNOTATION_PROCESSOR_ARTIFACTS = Set.of("hibernate-processor",
            "hibernate-search-processor");
    private static final String TARGET_POM = "quarkus-app-pom.xml";
    private static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    private static final String PROPERTY_START = "\\${";
    private static final String REPLACE_PROPERTY_START = "\\$REPLACE\\{";
    /**
     * List of plugins that should not be propagated from Quarkus QE TS / Examples to the tested app POM file.
     * This list could be probably smaller as we only care about actual test project, they are here in case someone,
     * does something unusually as it is unnecessary to propagate them.
     */
    private static final Set<String> IGNORED_PLUGINS = Set.of("maven-surefire-plugin", "maven-failsafe-plugin",
            "maven-javadoc-plugin", "jacoco-maven-plugin", "maven-source-plugin",
            "formatter-maven-plugin", "impsort-maven-plugin", "maven-checkstyle-plugin", "checkstyle",
            "quarkus-maven-plugin", MAVEN_COMPILER_PLUGIN);
    private static final String IO_QUARKUS = "io.quarkus";
    private static final String IO_QUARKUS_QE = "io.quarkus.qe";
    private static final String FAILSAFE_PLUGIN_VERSION = "failsafe-plugin.version";
    private static final String SUREFIRE_PLUGIN_VERSION = "surefire-plugin.version";
    /**
     * Properties to propagate to the target POM file.
     */
    private static final Set<String> POM_PROPERTIES = Set.of("quarkus.platform.group-id", "quarkus.platform.artifact-id",
            "quarkus.platform.version", "compiler-plugin.version", SUREFIRE_PLUGIN_VERSION, FAILSAFE_PLUGIN_VERSION);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    // IMPORTANT: this method only implements what we need right now, it cannot be perfect without effective POM
    @Override
    public void execute() throws MojoExecutionException {
        if (SKIP_INTEGRATION_TESTS || targetPomExists(project)) {
            return;
        }

        // better clone current project because it is very easy forget we must not change it
        // remember, whatever you change on 'this.project' will affect all plugins executed after this one
        var project = this.project.clone();

        deleteTargetPomIfExists(project);
        var rawCurrentProjectModel = getRawCurrentProjectModel(project);
        var newPomModel = getNewPomMavenModel();
        newPomModel.setArtifactId(project.getArtifactId());
        newPomModel.setVersion(project.getVersion());
        addCurrentProjectDependencies(newPomModel, rawCurrentProjectModel, project);
        addCurrentProjectPlugins(newPomModel, rawCurrentProjectModel, project);
        addCurrentProjectRepositories(newPomModel, rawCurrentProjectModel);
        propagateMavenPomProperties(newPomModel, project);
        saveToCurrentProjectTarget(newPomModel, project);
    }

    private static void deleteTargetPomIfExists(MavenProject project) {
        // this could be optimized, but for now, we always regenerate it in case properties changed
        if (targetPomExists(project)) {
            getTargetPomPath(project).toFile().delete();
        }
    }

    private static void propagateMavenPomProperties(Model newPomModel, MavenProject project) {
        POM_PROPERTIES.forEach(propertyKey -> {
            var propertyValue = System.getProperty(propertyKey);
            if (propertyValue == null) {
                propertyValue = project.getProperties().getProperty(propertyKey);
            }
            if (FAILSAFE_PLUGIN_VERSION.equals(propertyKey)) {
                if (propertyValue == null) {
                    propertyValue = project.getProperties().getProperty(SUREFIRE_PLUGIN_VERSION);
                }
                Objects.requireNonNull(propertyValue, "Could not find Failsafe Plugin Version, please set either '%s' or '%s'"
                        .formatted(FAILSAFE_PLUGIN_VERSION, SUREFIRE_PLUGIN_VERSION));
            } else {
                Objects.requireNonNull(propertyValue,
                        "POM file property '" + propertyKey + "' is required but could not found");
            }
            newPomModel.getProperties().setProperty(propertyKey, propertyValue);
        });
        if (project.getProperties().getProperty(MAVEN_COMPILER_RELEASE) != null) {
            newPomModel.getProperties().setProperty(MAVEN_COMPILER_RELEASE,
                    project.getProperties().getProperty(MAVEN_COMPILER_RELEASE));
        } else {
            newPomModel.getProperties().setProperty(MAVEN_COMPILER_RELEASE, Integer.toString(Runtime.version().feature()));
        }
    }

    private static void addCurrentProjectPlugins(Model newPomModel, Model rawCurrentProjectModel, MavenProject project) {
        // this intentionally takes only plugins from current project and not the parent and not the profiles
        // basically we only need plugins in very special cases, like if someone wants to generate gRPC stubs
        if (rawCurrentProjectModel.getBuild() == null || project.getBuild() == null
                || rawCurrentProjectModel.getBuild().getPlugins().isEmpty()
                || project.getBuild().getPlugins().isEmpty()) {
            return;
        }
        if (newPomModel.getBuild() == null) {
            newPomModel.setBuild(new Build());
        }

        rawCurrentProjectModel.getBuild().getPlugins().forEach(p -> customizeMavenCompilerPlugin(p, newPomModel));

        rawCurrentProjectModel.getBuild().getPlugins().stream()
                .filter(PreparePomMojo::isNotIgnoredPlugin)
                .map(p -> project.getBuild().getPlugins().stream()
                        .filter(p1 -> p1.getArtifactId().equalsIgnoreCase(p.getArtifactId())
                                && p1.getGroupId().equalsIgnoreCase(p.getGroupId()))
                        .findFirst().orElseThrow())
                .forEach(newPomModel.getBuild()::addPlugin);
    }

    private static void saveToCurrentProjectTarget(Model newPomModel, MavenProject project) throws MojoExecutionException {
        var targetPom = getTargetPomPath(project).toFile();
        if (!targetPom.exists()) {
            try {
                targetPom.createNewFile();
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create new Quarkus POM file " + targetPom, e);
            }
        }
        try (var newFileOS = new FileOutputStream(targetPom)) {
            new MavenXpp3Writer().write(newFileOS, newPomModel);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save '%s' POM file".formatted(targetPom), e);
        }

        // MavenXpp3 reader and writer expand '${property-key}', I didn't find escape that avoids that
        try {
            var pomFileContent = Files.readString(targetPom.toPath(), StandardCharsets.UTF_8);
            var updatedPomFileContent = pomFileContent.replaceAll(REPLACE_PROPERTY_START, PROPERTY_START);
            Files.writeString(targetPom.toPath(), updatedPomFileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to adjust '%s' POM file".formatted(targetPom), e);
        }
    }

    private static boolean targetPomExists(MavenProject project) {
        return Files.exists(getTargetPomPath(project));
    }

    private static void addCurrentProjectDependencies(Model newPomModel, Model rawCurrentProjectModel, MavenProject project) {
        for (Dependency dependency : project.getDependencies()) {
            if (isInTestScope(dependency)) {
                continue;
            }
            if (isQuarkusDependency(dependency) && hasNoHardcodedVersion(dependency, rawCurrentProjectModel)) {
                dependency.setVersion(null); // let the Quarkus dependency be managed by the BOM
            }

            // this is just to make things look like when developer writes it (leaves out scope)
            if ("compile".equalsIgnoreCase(dependency.getScope())) {
                dependency.setScope(null);
            }

            newPomModel.addDependency(dependency);
        }
    }

    private static boolean isQuarkusDependency(Dependency dependency) {
        // keep QE dependencies that are not in the test scope
        return dependency.getGroupId().startsWith(IO_QUARKUS) && !IO_QUARKUS_QE.equalsIgnoreCase(dependency.getGroupId());
    }

    /**
     * @return Model without resolved dependency versions etc. just loaded pom.xml of the current project
     */
    private static Model getRawCurrentProjectModel(MavenProject project) throws MojoExecutionException {
        var pomPath = project.getBasedir().toPath().resolve("pom.xml");
        try {
            return getMavenModel(new FileInputStream(pomPath.toFile()));
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Failed to load POM file from file path " + pomPath, e);
        }
    }

    private static boolean hasNoHardcodedVersion(Dependency dependency, Model rawCurrentProjectModel) {
        return rawCurrentProjectModel
                .getDependencies()
                .stream()
                .filter(d -> d.getVersion() != null && !d.getVersion().isBlank())
                .noneMatch(d -> d.getArtifactId().equalsIgnoreCase(dependency.getArtifactId())
                        && d.getGroupId().equalsIgnoreCase(dependency.getGroupId()));
    }

    private static Model getNewPomMavenModel() throws MojoExecutionException {
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream("quarkus-app-pom.xml");
        if (is == null) {
            throw new MojoExecutionException("Quarkus application base pom.xml not found");
        }
        return getMavenModel(is);
    }

    private static Model getMavenModel(InputStream is) throws MojoExecutionException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (is) {
            return reader.read(is);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create Maven model", e);
        }
    }

    private static boolean isInTestScope(Dependency dependency) {
        return "test".equalsIgnoreCase(dependency.getScope());
    }

    private static boolean isNotIgnoredPlugin(Plugin plugin) {
        return !IGNORED_PLUGINS.contains(plugin.getArtifactId());
    }

    private static void customizeMavenCompilerPlugin(Plugin plugin, Model newPomModel) {
        if (MAVEN_COMPILER_PLUGIN.equals(plugin.getArtifactId())) {
            boolean propagated = propagateAnnotationProcessors(newPomModel, plugin.getConfiguration());
            if (!propagated) {
                for (PluginExecution execution : plugin.getExecutions()) {
                    propagated = propagateAnnotationProcessors(newPomModel, execution.getConfiguration());
                    if (propagated) {
                        break;
                    }
                }
            }
        }
    }

    private static boolean propagateAnnotationProcessors(Model newPomModel, Object configObject) {
        if (configObject instanceof Xpp3Dom config) {
            var annotationProcessPaths = config.getChild("annotationProcessorPaths");
            if (annotationProcessPaths != null) {
                for (Xpp3Dom path : annotationProcessPaths.getChildren()) {
                    var artifactId = path.getChild("artifactId");
                    if (artifactId != null && PROPAGATED_ANNOTATION_PROCESSOR_ARTIFACTS.contains(artifactId.getValue())) {
                        var mavenCompilerPlugin = newPomModel.getBuild().getPlugins().stream()
                                .filter(p -> MAVEN_COMPILER_PLUGIN.equalsIgnoreCase(p.getArtifactId()))
                                .findFirst().orElseThrow(() -> new IllegalStateException(
                                        MAVEN_COMPILER_PLUGIN + " must be present in new POM model"));
                        mavenCompilerPlugin.setConfiguration(config);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void addCurrentProjectRepositories(Model newPomModel, Model rawCurrentProjectModel) {
        if (rawCurrentProjectModel.getRepositories() != null && !rawCurrentProjectModel.getRepositories().isEmpty()) {
            rawCurrentProjectModel.getRepositories().forEach(newPomModel::addRepository);
        }
    }

    private static Path getTargetPomPath(MavenProject project) {
        return getCurrentProjectTarget(project).resolve(TARGET_POM);
    }

    private static Path getCurrentProjectTarget(MavenProject project) {
        var targetPath = project.getBasedir().toPath().resolve("target");
        if (!Files.exists(targetPath)) {
            targetPath.toFile().mkdirs();
        }
        return targetPath;
    }
}
