package io.quarkus.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.smallrye.common.os.OS;

public abstract class QuarkusCLIUtils {
    public static final String RESOURCES_DIR = Paths.get("src", "main", "resources").toString();
    public static final String PROPERTIES_FILE = "application.properties";
    public static final String PROPERTIES_YAML_FILE = "application.yml";
    public static final String POM_FILE = "pom.xml";
    private static final String ANSI_BOLD_TEXT_ESCAPE_SEQ = "[1m";
    private static final char ESCAPE_CHARACTER = 27;

    /**
     * This constant stands for number of fields in groupId:artifactId:version string, when separated via ":".
     * Checkstyle doesn't allow to have a number directly in a code, so this needs to be a constant.
     */
    private static final int GAV_FIELDS_LENGTH = 3;

    public static IQuarkusCLIAppManager createAppManager(QuarkusCliClient cliClient,
            DefaultArtifactVersion oldVersionStream,
            DefaultArtifactVersion newVersionStream) {
        if (QuarkusProperties.isRHBQ()) {
            return new RHBQPlatformAppManager(cliClient, oldVersionStream, newVersionStream,
                    new DefaultArtifactVersion(QuarkusProperties.getVersion()));
        }
        return new DefaultQuarkusCLIAppManager(cliClient, oldVersionStream, newVersionStream);
    }

    /**
     * Create app, put properties into application.properties file,
     * then update the app and verify that properties are the expected ones.
     *
     * @param appManager Manager to produce and update app
     * @param oldProperties Properties to put into application before update.
     *        These properties should not be present after update.
     * @param expectedNewProperties Properties which should be in app after update.
     */
    public static void checkPropertiesUpdate(IQuarkusCLIAppManager appManager,
            Properties oldProperties, Properties expectedNewProperties) throws IOException {
        QuarkusCliRestService app = appManager.createApplication();
        writePropertiesToPropertiesFile(app, oldProperties);

        appManager.updateApp(app);
        Properties newProperties = readPropertiesFile(app);

        verifyProperties(newProperties, oldProperties, expectedNewProperties);
    }

    /**
     * Create app, put properties into application.yml file,
     * then update the app and verify that properties are the expected ones.
     *
     * @param appManager Manager to produce and update app
     * @param oldProperties Properties to put into application before update.
     *        These properties should not be present after update.
     * @param expectedNewProperties Properties which should be in app after update.
     */
    public static void checkYamlPropertiesUpdate(IQuarkusCLIAppManager appManager,
            Properties oldProperties,
            Properties expectedNewProperties) throws IOException {
        // create app with yaml extension
        QuarkusCliRestService app = appManager.createApplicationWithExtensions("quarkus-config-yaml");
        // write properties to yaml
        writePropertiesToYamlFile(app, oldProperties);

        appManager.updateApp(app);

        Properties properties = readPropertiesYamlFile(app);
        verifyProperties(properties, oldProperties, expectedNewProperties);
    }

    private static void verifyProperties(Properties actualProperties,
            Properties oldProperties, Properties expectedNewProperties) {
        for (Map.Entry<Object, Object> entry : expectedNewProperties.entrySet()) {
            assertTrue(actualProperties.containsKey(entry.getKey()),
                    "Properties after update does not contain " + entry.getKey());
            assertEquals(entry.getValue(), actualProperties.get(entry.getKey()),
                    "Property " + entry.getKey() + " does not match after update");
        }

        for (Map.Entry<Object, Object> entry : oldProperties.entrySet()) {
            assertFalse(actualProperties.containsKey(entry.getKey()),
                    "Properties after update should not contain " + entry.getKey());
        }
    }

    /**
     * Create app, put dependencies into it, update it and check new dependencies are present.
     * Use {@link QuarkusDependency} it has .equals method properly set.
     *
     * @param oldDependencies Dependencies to put into app before update. Use {@link QuarkusDependency}.
     *        These dependencies are expected to not be in app after update.
     * @param newDependencies Dependencies to expect after update. Use {@link QuarkusDependency}.
     */
    public static void checkDependenciesUpdate(IQuarkusCLIAppManager appManager,
            List<Dependency> oldDependencies, List<Dependency> newDependencies)
            throws XmlPullParserException, IOException {
        QuarkusCliRestService app = appManager.createApplication();
        addDependenciesToPom(app, oldDependencies);

        appManager.updateApp(app);

        List<Dependency> actualDependencies = getDependencies(app);
        oldDependencies.forEach(dependency -> assertFalse(actualDependencies.contains(dependency),
                "Pom.xml after update should not contain dependency: " + dependency));
        newDependencies.forEach(dependency -> assertTrue(actualDependencies.contains(dependency),
                "Pom.xml after update should contain dependency: " + dependency));
    }

    /**
     * Create app, put plugins into it, update it and check new plugins are present.
     * Use {@link QuarkusPlugin} it has .equals method properly set.
     *
     * @param oldPlugins Plugin to put into app before update. Use {@link QuarkusPlugin}.
     *        These plugins are expected to not be in app after update
     * @param newPlugins Plguins to expect after update. Use {@link QuarkusPlugin}.
     */
    public static void checkPluginUpdate(IQuarkusCLIAppManager appManager,
            List<Plugin> oldPlugins, List<Plugin> newPlugins)
            throws XmlPullParserException, IOException {
        QuarkusCliRestService app = appManager.createApplication();
        addPluginsToPom(app, oldPlugins);

        appManager.updateApp(app);

        List<Plugin> actualPlugins = getPlugins(app);
        oldPlugins.forEach(plugin -> assertFalse(actualPlugins.contains(plugin),
                "Pom.xml after update should not contain plugin " + plugin));
        newPlugins.forEach(plugin -> assertTrue(actualPlugins.contains(plugin),
                "Pom.xml after update should contain plugin " + plugin));
    }

    /**
     * Write properties into app's application.properties file.
     */
    public static void writePropertiesToPropertiesFile(QuarkusCliRestService app, Properties properties) throws IOException {
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
    public static void writePropertiesToYamlFile(QuarkusCliRestService app, Properties properties) throws IOException {
        File yaml = getPropertiesYamlFile(app);
        YamlPropertiesHandler.writePropertiesIntoYaml(yaml, properties);
    }

    public static Properties readPropertiesFile(QuarkusCliRestService app) throws IOException {
        return loadPropertiesFromFile(getPropertiesFile(app));
    }

    public static Properties readPropertiesYamlFile(QuarkusCliRestService app) throws IOException {
        File yamlFile = getPropertiesYamlFile(app);
        return YamlPropertiesHandler.readYamlFileIntoProperties(yamlFile);
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
     * Change given properties in app's pom.
     * Other properties are unchanged.
     */
    public static void changePropertiesInPom(QuarkusCliRestService app, Properties properties)
            throws XmlPullParserException, IOException {
        Model pom = getPom(app);
        Properties pomProperties = pom.getProperties();
        pomProperties.putAll(properties);
        pom.setProperties(pomProperties);
        savePom(app, pom);
    }

    /**
     * If tests are not on RHBQ it will set properties in app's pom to work with community quarkus BOM.
     * Expects that app is using RHBQ by default.
     */
    public static void setCommunityBomIfNotRunningRHBQ(QuarkusCliRestService app, String communityQuarkusVersion)
            throws XmlPullParserException, IOException {
        if (!QuarkusProperties.getVersion().contains("redhat")) {
            Properties communityBomProperties = new Properties();
            communityBomProperties.put("quarkus.platform.group-id", "io.quarkus.platform");
            communityBomProperties.put("quarkus.platform.version", communityQuarkusVersion);
            changePropertiesInPom(app, communityBomProperties);
        }
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
        File pomfile = app.getFileFromApplication(subdir, POM_FILE);
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        XmlStreamReader streamReader = new XmlStreamReader(pomfile);
        return mavenReader.read(streamReader);
    }

    public static void savePom(QuarkusCliRestService app, Model model) throws IOException {
        OutputStream output = new FileOutputStream(app.getFileFromApplication(POM_FILE));
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

    /**
     * Escapes a command-line secret chars for Windows OS.
     */
    public static String escapeSecretCharsForWindows(String secret) {
        return "\"" + secret
                .replace("\"", "\\\"")
                + "\"";
    }

    /**
     * When Quarkus CLI prints out text, especially by {@code quarkus config encrypt} command,
     * important parts (like encoded secrets) can be highlighted or there can be hidden chars.
     * We recognize hidden chars etc. This method handles both situation. It's definitely imperfect,
     * but we only deal with scenarios (issues) we run on.
     */
    public static String removeAnsiAndHiddenChars(String text) {
        if (OS.current() == OS.WINDOWS) {
            var result = text
                    .trim()
                    .transform(t -> {
                        if (t.contains(ANSI_BOLD_TEXT_ESCAPE_SEQ)) {
                            return t.substring(ANSI_BOLD_TEXT_ESCAPE_SEQ.length());
                        }
                        return t;
                    })
                    .transform(t -> {
                        int idx = t.indexOf(ESCAPE_CHARACTER);
                        if (idx >= 0) {
                            return t.substring(0, idx);
                        }
                        return t;
                    });
            return result;
        }
        return text;
    }

    public static String toUtf8(String t) {
        return new String(t.getBytes(Charset.defaultCharset()), StandardCharsets.UTF_8);
    }
}
