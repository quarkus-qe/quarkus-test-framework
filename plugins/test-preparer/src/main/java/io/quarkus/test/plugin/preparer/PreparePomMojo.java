package io.quarkus.test.plugin.preparer;

import static io.quarkus.test.plugin.preparer.PreparerTestUtils.SKIP_INTEGRATION_TESTS;
import static io.quarkus.test.plugin.preparer.PreparerTestUtils.getTargetPomPath;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

// if you ever get checkstyle line length warning for the @Mojo, adjust checkstyle-suppressions.xml
@Mojo(name = "prepare-pom-mojo", defaultPhase = PACKAGE, requiresDependencyCollection = COMPILE, requiresDependencyResolution = COMPILE, threadSafe = true)
public class PreparePomMojo extends AbstractMojo {

    private static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    private static final String PROPERTY_START = "\\${";
    private static final String REPLACE_PROPERTY_START = "\\$REPLACE\\{";
    /**
     * List of plugins that should not be propagated from Quarkus QE TS / Examples to the tested app POM file.
     * This list could be probably smaller as we only care about actual test project, they are here in case someone,
     * does something unusually as it is unnecessary to propagate them.
     */
    private static final Set<String> IGNORED_PLUGINS = Set.of("maven-surefire-plugin", "maven-failsafe-plugin",
            "maven-javadoc-plugin", "jacoco-maven-plugin", "maven-compiler-plugin", "maven-source-plugin",
            "formatter-maven-plugin", "impsort-maven-plugin", "maven-checkstyle-plugin", "checkstyle",
            "quarkus-maven-plugin");
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
        if (SKIP_INTEGRATION_TESTS || targetPomExists()) {
            return;
        }

        deleteTargetPomIfExists();
        var rawCurrentProjectModel = getRawCurrentProjectModel();
        var newPomModel = getNewPomMavenModel();
        newPomModel.setArtifactId(project.getArtifactId());
        newPomModel.setVersion(project.getVersion());
        addCurrentProjectDependencies(newPomModel, rawCurrentProjectModel);
        addCurrentProjectPlugins(newPomModel, rawCurrentProjectModel);
        addCurrentProjectRepositories(newPomModel, rawCurrentProjectModel);
        propagateMavenPomProperties(newPomModel);
        saveToCurrentProjectTarget(newPomModel);
    }

    private void deleteTargetPomIfExists() {
        // this could be optimized, but for now, we always regenerate it in case properties changed
        if (targetPomExists()) {
            getTargetPomPath(project).toFile().delete();
        }
    }

    private void propagateMavenPomProperties(Model newPomModel) {
        POM_PROPERTIES.forEach(propertyKey -> {
            var propertyValue = System.getProperty(propertyKey);
            if (propertyValue == null) {
                propertyValue = this.project.getProperties().getProperty(propertyKey);
            }
            if (FAILSAFE_PLUGIN_VERSION.equals(propertyKey)) {
                if (propertyValue == null) {
                    propertyValue = this.project.getProperties().getProperty(SUREFIRE_PLUGIN_VERSION);
                }
                Objects.requireNonNull(propertyValue, "Could not find Failsafe Plugin Version, please set either '%s' or '%s'"
                        .formatted(FAILSAFE_PLUGIN_VERSION, SUREFIRE_PLUGIN_VERSION));
            } else {
                Objects.requireNonNull(propertyValue,
                        "POM file property '" + propertyKey + "' is required but could not found");
            }
            newPomModel.getProperties().setProperty(propertyKey, propertyValue);
            if (this.project.getProperties().getProperty(MAVEN_COMPILER_RELEASE) != null) {
                newPomModel.getProperties().setProperty(MAVEN_COMPILER_RELEASE,
                        this.project.getProperties().getProperty(MAVEN_COMPILER_RELEASE));
            } else {
                newPomModel.getProperties().setProperty(MAVEN_COMPILER_RELEASE, Integer.toString(Runtime.version().feature()));
            }
        });
    }

    private void addCurrentProjectPlugins(Model newPomModel, Model rawCurrentProjectModel) {
        // this intentionally takes only plugins from current project and not the parent and not the profiles
        // basically we only need plugins in very special cases, like if someone wants to generate gRPC stubs
        if (rawCurrentProjectModel.getBuild() == null || this.project.getBuild() == null
                || rawCurrentProjectModel.getBuild().getPlugins().isEmpty()
                || this.project.getBuild().getPlugins().isEmpty()) {
            return;
        }
        if (newPomModel.getBuild() == null) {
            newPomModel.setBuild(new Build());
        }
        rawCurrentProjectModel.getBuild().getPlugins().stream()
                .filter(PreparePomMojo::isNotIgnoredPlugin)
                .map(p -> this.project.getBuild().getPlugins().stream()
                        .filter(p1 -> p1.getArtifactId().equalsIgnoreCase(p.getArtifactId())
                                && p1.getGroupId().equalsIgnoreCase(p.getGroupId()))
                        .findFirst().orElseThrow())
                .forEach(newPomModel.getBuild()::addPlugin);
    }

    private void saveToCurrentProjectTarget(Model newPomModel) throws MojoExecutionException {
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

    private boolean targetPomExists() {
        return Files.exists(getTargetPomPath(project));
    }

    private void addCurrentProjectDependencies(Model newPomModel, Model rawCurrentProjectModel) {
        for (Dependency dependency : this.project.getDependencies()) {
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

    private boolean isQuarkusDependency(Dependency dependency) {
        // keep QE dependencies that are not in the test scope
        return dependency.getGroupId().startsWith(IO_QUARKUS) && !IO_QUARKUS_QE.equalsIgnoreCase(dependency.getGroupId());
    }

    /**
     * @return Model without resolved dependency versions etc. just loaded pom.xml of the current project
     */
    private Model getRawCurrentProjectModel() throws MojoExecutionException {
        var pomPath = this.project.getBasedir().toPath().resolve("pom.xml");
        try {
            return getMavenModel(new FileInputStream(pomPath.toFile()));
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Failed to load POM file from file path " + pomPath, e);
        }
    }

    private boolean hasNoHardcodedVersion(Dependency dependency, Model rawCurrentProjectModel) {
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

    private static void addCurrentProjectRepositories(Model newPomModel, Model rawCurrentProjectModel) {
        if (rawCurrentProjectModel.getRepositories() != null && !rawCurrentProjectModel.getRepositories().isEmpty()) {
            rawCurrentProjectModel.getRepositories().forEach(newPomModel::addRepository);
        }
    }
}
