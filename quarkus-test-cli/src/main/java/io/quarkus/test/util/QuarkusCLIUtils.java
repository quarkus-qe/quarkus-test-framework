package io.quarkus.test.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import io.quarkus.test.bootstrap.QuarkusCliRestService;

public abstract class QuarkusCLIUtils {
    public static final String RESOURCES_DIR = "src/main/resources";
    public static final String PROPERTIES_FILE = "application.properties";
    public static final String PROPERTIES_YAML_FILE = "application.yml";

    /**
     * This constant stands for number of fields in groupId:artifactId:version string, when separated via ":".
     * Checkstyle doesn't allow to have a number directly in a code, si this needs to be a constant.
     */
    private static final int GAV_FIELDS_LENGTH = 3;

    /**
     * Write properties into app's application.properties file.
     */
    public static void writePropertiesToApp(QuarkusCliRestService app, Properties properties) throws IOException {
        File propertiesFile = getPropertiesFile(app);
        BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile));

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            writer.append(entry.getKey().toString());
            writer.append("=");
            writer.append(entry.getValue().toString());
            writer.append("\n");
        }
        writer.close();
    }

    /**
     * Write properties into app's application.yml file.
     */
    public static void writePropertiesToYaml(QuarkusCliRestService app, Properties properties) throws IOException {
        File yaml = getPropertiesYamlFile(app);
        // we're using print writer to overwrite existing content of the file
        PrintWriter writer = new PrintWriter(new FileWriter(yaml));
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            writer.append(entry.getKey().toString());
            writer.append(": ");
            writer.append(entry.getValue().toString());
            writer.append("\n");
        }
        writer.close();
    }

    public static Properties readPropertiesFile(QuarkusCliRestService app) throws IOException {
        return loadPropertiesFromFile(getPropertiesFile(app));
    }

    public static Properties readPropertiesYamlFile(QuarkusCliRestService app) throws IOException {
        return loadPropertiesFromFile(getPropertiesYamlFile(app));
    }

    public static Properties loadPropertiesFromFile(File file) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(file));
        return properties;
    }

    public static File getPropertiesFile(QuarkusCliRestService app) {
        return app.getFileFromApplication(RESOURCES_DIR, PROPERTIES_FILE);
    }

    public static File getPropertiesYamlFile(QuarkusCliRestService app) {
        return app.getFileFromApplication(RESOURCES_DIR, PROPERTIES_YAML_FILE);
    }

    /**
     * Takes content of a file and check that in it's content all keys are renamed to values.
     * It's supposed for quarkus update testing of stuff like rename method names or package imports,
     * where we can easily check that e.g. import javax.security.cert was renamed to java.security.cert.
     *
     * @param file File to check content of
     * @param renames map of renames string. Asserts that map keys should be renamed to their corresponding values.
     */
    public static void checkRenamesInFile(File file, Map<String, String> renames) throws IOException {
        String content = Files.readString(file.toPath());

        for (Map.Entry<String, String> entry : renames.entrySet()) {
            String failMessage = entry.getKey() + " should be renamed to " + entry.getValue();
            assertFalse(content.contains(entry.getKey()), failMessage);
            assertTrue(content.contains(entry.getValue()), failMessage);
        }
    }

    /**
     * Reads quarkus version from app's pom.
     */
    public static DefaultArtifactVersion getQuarkusAppVersion(QuarkusCliRestService app)
            throws IOException, XmlPullParserException {
        return new DefaultArtifactVersion(getPom(app).getProperties().getProperty("quarkus.platform.version"));
    }

    public static void addDependenciesToPom(QuarkusCliRestService app, List<Dependency> dependencies)
            throws XmlPullParserException, IOException {
        Model pom = getPom(app);
        dependencies.forEach(pom::addDependency);
        savePom(app, pom);
    }

    public static void addPluginsToPom(QuarkusCliRestService app, List<Plugin> plugins)
            throws XmlPullParserException, IOException {
        Model pom = getPom(app);
        plugins.forEach(plugin -> pom.getBuild().addPlugin(plugin));
        savePom(app, pom);
    }

    /**
     * Get plugins from app's pom.
     * Does not read pluginManagement.
     */
    public static List<Plugin> getPlugins(QuarkusCliRestService app) throws XmlPullParserException, IOException {
        return getPom(app).getBuild().getPlugins();
    }

    /**
     * Get dependencies from app's pom.
     * Does not read dependencyManagement.
     */
    public static List<Dependency> getDependencies(QuarkusCliRestService app) throws XmlPullParserException, IOException {
        return getPom(app).getDependencies();
    }

    /**
     * Get properties defined in app's pom.
     */
    public static Properties getProperties(QuarkusCliRestService app) throws XmlPullParserException, IOException {
        return getPom(app).getProperties();
    }

    /**
     * Get main pom of the application (the one in root dir).
     */
    public static Model getPom(QuarkusCliRestService app) throws XmlPullParserException, IOException {
        return getPom(app, "");
    }

    /**
     * Get pom of the application.
     * Specifying subdir param can be used to get pom of nested module (in case of multi-module apps).
     */
    public static Model getPom(QuarkusCliRestService app, String subdir) throws IOException, XmlPullParserException {
        File pomfile = app.getFileFromApplication(subdir, "pom.xml");
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        XmlStreamReader streamReader = new XmlStreamReader(pomfile);
        return mavenReader.read(streamReader);
    }

    public static void savePom(QuarkusCliRestService app, Model model) throws IOException {
        OutputStream output = new FileOutputStream(app.getFileFromApplication("pom.xml"));
        new MavenXpp3Writer().write(output, model);
    }

    /**
     * Extend upstream Dependency class to better suit our needs.
     */
    public static class QuarkusDependency extends Dependency {

        /**
         * Constructor, which parses groupId:ArtifactId:Version into class.
         * Version part is optional.
         * Argument can be e.g. "org.graalvm.nativeimage:svm:24.0.1" or "io.quarkus:quarkus-rest-client"
         */
        public QuarkusDependency(String groupArtifactVersion) {
            String[] fields = groupArtifactVersion.split(":");
            setGroupId(fields[0]);
            setArtifactId(fields[1]);
            if (fields.length == GAV_FIELDS_LENGTH) {
                setVersion(fields[2]);
            }
        }

        /**
         * Original Dependency class does not implement "equals" method, so we cannot easily check if some dependency is
         * contained somewhere
         * Implementing this method so we can easily detect dependencies in collections etc.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Dependency dependency)) {
                return false;
            }
            if (!(dependency.getArtifactId().equals(this.getArtifactId())
                    && dependency.getGroupId().equals(this.getGroupId()))) {
                return false;
            }
            if (this.getVersion() != null && dependency.getVersion() != null) {
                return dependency.getVersion().equals(this.getVersion());
            }
            return this.getVersion() == null && dependency.getVersion() == null;
        }

        /**
         * Overriding hash code is required by checkStyle if .equals is overridden.
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    public static class QuarkusPlugin extends Plugin {
        /**
         * Constructor, which parses groupId:ArtifactId:Version into class.
         * Version part is optional.
         * Argument can be e.g. "org.apache.maven.plugins:maven-compiler-plugin:3.10.0"
         */
        public QuarkusPlugin(String groupArtifactVersion) {
            String[] fields = groupArtifactVersion.split(":");
            setGroupId(fields[0]);
            setArtifactId(fields[1]);
            if (fields.length == GAV_FIELDS_LENGTH) {
                setVersion(fields[2]);
            }
        }

        /**
         * Parent Plugin class only compares groupId and artifactId in .equals.
         * Implementing this method so we can easily detect and distinguish plugins in collections etc.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Plugin plugin)) {
                return false;
            }
            if (!(plugin.getArtifactId().equals(this.getArtifactId())
                    && plugin.getGroupId().equals(this.getGroupId()))) {
                return false;
            }
            if (this.getVersion() != null && plugin.getVersion() != null) {
                return plugin.getVersion().equals(this.getVersion());
            }
            return this.getVersion() == null && plugin.getVersion() == null;
        }

        /**
         * Overriding hash code is required by checkStyle if .equals is overridden.
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }

        /**
         * Override toString to also print version, parent class is not doing that.
         */
        @Override
        public String toString() {
            return "Plugin {groupId=" + getGroupId() + ", artifactId=" + getArtifactId() + ", version=" + getVersion() + "}";
        }
    }
}
