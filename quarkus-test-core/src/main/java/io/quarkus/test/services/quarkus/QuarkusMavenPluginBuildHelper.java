package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder.TARGET;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_GROUP_ID;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_VERSION;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLUGIN_VERSION;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.getPluginVersion;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.getVersion;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.isNativeEnabled;
import static io.quarkus.test.utils.FileUtils.findTargetFile;
import static io.quarkus.test.utils.MavenUtils.MVN_REPOSITORY_LOCAL;
import static io.quarkus.test.utils.PropertiesUtils.SLASH;
import static io.quarkus.test.utils.PropertiesUtils.toMvnSystemProperty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.TestExecutionProperties;
import io.smallrye.common.os.OS;

public final class QuarkusMavenPluginBuildHelper {

    private static final int LOOKUP_PARENT_DIR_LIMIT = 5;
    private static final Set<String> IGNORED_BUILD_ARGS = Set.of("maven.failsafe.debug", "quarkus.build.skip", "reruns");
    private static final String NATIVE_RUNNER = "-runner";
    private static final String EXE = ".exe";
    private static final String TARGET_POM = "quarkus-app-pom.xml";
    private static final String JVM_RUNNER = "-runner.jar";
    private static final String QUARKUS_APP = "quarkus-app";
    private static final String QUARKUS_RUN = "quarkus-run.jar";
    private static final String CUSTOM_RUNNER_DIR = "custom-build";
    private static final Set<String> FAILSAFE_BUILD_LIFECYCLE_PHASES = Set.of("verify", "install", "deploy",
            "integration-test");
    // following constant helps us to detect args specified before lifecycle phase like `mvn -Dstuff verify`
    // TODO: this should probably be more robust
    private static final String LAUNCHER = "org.codehaus.plexus.classworlds.launcher.Launcher";
    private static final String JAVA_SUFFIX = ".java";
    private static final String POM_XML = "pom.xml";
    private final Path appFolder;
    private final Set<String> appClassNames;
    private final List<Dependency> forcedDependencies;
    private final boolean buildWithAllClasses;
    private final Path targetFolderForLocalArtifacts;
    private final QuarkusApplicationManagedResourceBuilder resourceBuilder;
    private final String artifactSuffix;
    private final Mode mode;
    // dependencies that may already be present but if they are not, we must add them
    // this is to make build backports compatible for what used to be 'deploy-to-openshift-using-extension' we activated
    // ideally we drop this and users activate OpenShift profile when they need it
    private final List<Dependency> requiredDependencies;

    QuarkusMavenPluginBuildHelper(QuarkusApplicationManagedResourceBuilder resourceBuilder) {
        this(resourceBuilder, null, null);
    }

    QuarkusMavenPluginBuildHelper(QuarkusApplicationManagedResourceBuilder resourceBuilder,
            Path targetFolderForLocalArtifacts, String artifactSuffix) {
        this(resourceBuilder, targetFolderForLocalArtifacts, artifactSuffix, List.of());
    }

    QuarkusMavenPluginBuildHelper(QuarkusApplicationManagedResourceBuilder resourceBuilder,
            Path targetFolderForLocalArtifacts, String artifactSuffix, List<Dependency> requiredDependencies) {
        requireNonNull(resourceBuilder);
        if (!requiredDependencies.isEmpty()) {
            var newPomAsString = requireNonNull(FileUtils.loadFile(getPomFileCreatedByOurPlugin()));
            this.requiredDependencies = requiredDependencies.stream()
                    .filter(d -> !newPomAsString.contains("<artifactId>" + d.artifactId() + "</artifactId>"))
                    .toList();
        } else {
            this.requiredDependencies = List.of();
        }
        this.resourceBuilder = resourceBuilder;
        this.appFolder = resourceBuilder.getApplicationFolder();
        this.appClassNames = Arrays.stream(resourceBuilder.getAppClasses()).map(Class::getName).collect(toUnmodifiableSet());
        this.buildWithAllClasses = resourceBuilder.isBuildWithAllClasses();
        this.targetFolderForLocalArtifacts = targetFolderForLocalArtifacts;
        this.artifactSuffix = artifactSuffix;
        this.forcedDependencies = List.copyOf(resourceBuilder.getForcedDependencies());
        this.mode = isNativeEnabled(resourceBuilder.getContext()) ? Mode.NATIVE : Mode.JVM;
    }

    public static void deleteNativeExecutablesInPermanentLocation() {
        var customNativeExecutablePath = Path.of(TARGET).resolve(CUSTOM_RUNNER_DIR);
        if (Files.exists(customNativeExecutablePath)) {
            try {
                FileUtils.deletePath(customNativeExecutablePath);
            } catch (Exception ex) {
                Log.warn("Could not delete folder with custom native executables. Caused by " + ex.getMessage());
            }
        }
    }

    private static List<String> findMavenCommandLineArgs() {
        List<String> mvnArgs = new ArrayList<>();
        ProcessHandle processHandle = ProcessHandle.current();
        boolean isMvnSettings = false;
        do {
            if (processHandle.info().arguments().isPresent()) {
                String[] args = processHandle.info().arguments().get();
                boolean keepArgs = false;
                for (String arg : args) {
                    if (arg == null) {
                        // this is probably not necessary, but let's stay on the safe side
                        continue;
                    }
                    if (isMvnSettings) {
                        isMvnSettings = false;
                        mvnArgs.add("-s");
                        mvnArgs.add(getMvnSettingsPath(arg));
                    }
                    if (keepArgs && arg.startsWith("-D")) {
                        if (isNotIgnoredArgument(arg)) {
                            mvnArgs.add(arg);
                        }
                    } else if (LAUNCHER.equals(arg) || FAILSAFE_BUILD_LIFECYCLE_PHASES.contains(arg)) {
                        // order is important here - we only want to propagate mvn args
                        keepArgs = true;
                    } else if ("-s".equals(arg)) {
                        isMvnSettings = true;
                    }
                }
                if (keepArgs) {
                    return mvnArgs;
                }
            }
            processHandle = processHandle.parent().orElse(null);
        } while (processHandle != null);
        throw new IllegalStateException("Failed to detect mvn command line arguments");
    }

    private static String getMvnSettingsPath(String arg) {
        var path = Path.of(arg).toAbsolutePath();
        if (!Files.exists(path)) {
            var parentFolder = Path.of(".").toAbsolutePath();
            // relative settings.xml path could be relative to the root but not the working dir
            for (int i = 0; i < LOOKUP_PARENT_DIR_LIMIT; i++) {
                var pathRelativeToFolder = parentFolder.resolve(arg).toAbsolutePath();
                if (!Files.exists(pathRelativeToFolder)) {
                    parentFolder = parentFolder.getParent();
                    if (parentFolder == null) {
                        break;
                    }
                } else {
                    path = pathRelativeToFolder;
                    break;
                }
            }
            if (!Files.exists(path)) {
                throw new RuntimeException("Failed to find Maven settings file for relative path: " + arg);
            }
        }
        return path.toString();
    }

    private static boolean isNotIgnoredArgument(String arg) {
        return IGNORED_BUILD_ARGS.stream().noneMatch(a -> ("-D" + a).equalsIgnoreCase(arg));
    }

    void prepareApplicationFolder() {
        prepareMavenProject(appFolder);
        resourceBuilder.copyResourcesToAppFolder();
    }

    private Path prepareMavenProject(Path mavenBuildProjectRoot) {
        // create new project root
        FileUtils.recreateDirectory(mavenBuildProjectRoot);

        // copy POM file created by our POM preparer plugin to the new project root
        var pomFileCreatedByOurPlugin = getPomFileCreatedByOurPlugin();
        FileUtils.copyFileTo(pomFileCreatedByOurPlugin, mavenBuildProjectRoot);
        Path newPom = mavenBuildProjectRoot.resolve(POM_XML);
        if (!isS2iScenario()) {
            mavenBuildProjectRoot.resolve(TARGET_POM).toFile().renameTo(newPom.toFile());
        }

        boolean pomAdjusted = false;
        // adjust pom.xml
        Document pomDocument = getDocument(newPom);
        Node projectElement = pomDocument.getElementsByTagName("project").item(0);
        if (OS.WINDOWS.isCurrent()) {
            // it's necessary to keep executable name small as I experienced LNK1104 error on Windows from long path
            // see also: https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation
            setProjectBuildNameToApp(projectElement, pomDocument);
            pomAdjusted = true;
        }
        // add forced dependencies
        if (!forcedDependencies.isEmpty()) {
            addForcedDependenciesToNewPom(pomDocument, projectElement);
            pomAdjusted = true;
        }
        if (!requiredDependencies.isEmpty()) {
            var newPomAsString = requireNonNull(FileUtils.loadFile(newPom.toFile()));
            var requiredMissingDependencies = requiredDependencies.stream()
                    .filter(d -> !newPomAsString.contains("<artifactId>" + d.artifactId() + "</artifactId>"))
                    .toList();
            if (!requiredMissingDependencies.isEmpty()) {
                addDependenciesToNewPom(pomDocument, projectElement, requiredMissingDependencies);
                pomAdjusted = true;
            }
        }

        if (pomAdjusted) {
            // update pom.xml file
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.transform(new DOMSource(pomDocument), new StreamResult(newPom.toFile()));
            } catch (TransformerException e) {
                throw new RuntimeException("Failed to persist enhanced POM file: " + e.getMessage());
            }
        }

        // add application classes to src/main/..
        addAppClassesToSrcMain(mavenBuildProjectRoot);

        // add enhanced application.properties and META-INF to the new project
        addEnhancedAppPropsAndMetaInf(mavenBuildProjectRoot);

        return mavenBuildProjectRoot;
    }

    private File getPomFileCreatedByOurPlugin() {
        var pomFileCreatedByOurPlugin = Path.of(TARGET).resolve(isS2iScenario() ? POM_XML : TARGET_POM).toFile();
        if (!pomFileCreatedByOurPlugin.exists()) {
            throw new RuntimeException(("Could not find '%s' POM file, please add 'io.quarkus.qe:quarkus-test-preparer'"
                    + " plugin").formatted(pomFileCreatedByOurPlugin));
        }
        return pomFileCreatedByOurPlugin;
    }

    private boolean isS2iScenario() {
        return resourceBuilder.isS2iScenario();
    }

    private Path buildOrReuseJar() {
        return buildOrReuseArtifact(List.of()).orElseThrow(() -> new RuntimeException("Failed to build JAR artifact"));
    }

    private Path buildOrReuseNativeExecutable() {
        return buildOrReuseArtifact(List.of()).orElseThrow(() -> new RuntimeException("Failed to build native executable"));
    }

    Path buildOrReuseArtifact() {
        return switch (mode) {
            case JVM -> buildOrReuseJar();
            case NATIVE -> buildOrReuseNativeExecutable();
        };
    }

    Optional<Path> buildOrReuseArtifact(Collection<String> additionalArgs) {
        requireNonNull(targetFolderForLocalArtifacts);

        final Path mavenBuildProjectRoot;
        if (isS2iScenario()) {
            // no adjustments it would be too complex, and we couldn't gain anything there
            mavenBuildProjectRoot = appFolder;
        } else {
            mavenBuildProjectRoot = prepareMavenProject(appFolder.resolve("mvn-build"));
        }

        return getArtifact().or(() -> buildArtifactWithQuarkusMvnPlugin(mavenBuildProjectRoot, additionalArgs));
    }

    private Optional<Path> getArtifact() {
        Optional<String> artifactLocation = Optional.empty();
        final Path targetFolder = targetFolderForLocalArtifacts;
        if (artifactSuffix != null) {
            var possiblyArtifact = findTargetFile(targetFolder, artifactSuffix).map(Path::of);
            if (possiblyArtifact.isPresent()) {
                return possiblyArtifact;
            }
            throw new IllegalStateException(String.format("Folder %s doesn't contain '%s'", targetFolder, artifactSuffix));
        }
        resourceBuilder.createSnapshotOfBuildPropertiesIfNotExists();
        if (!resourceBuilder.buildPropertiesChanged()) {
            if (mode == Mode.NATIVE) {
                // custom native executable has different name, therefore we can safely re-use it
                artifactLocation = findNativeBuildExecutable(targetFolder, isCustomBuildRequired(), appFolder);
            } else if (!isCustomBuildRequired()) {
                artifactLocation = findJvmArtifact(targetFolder);
            }
        }

        return artifactLocation.map(Path::of);
    }

    private Optional<Path> buildArtifactWithQuarkusMvnPlugin(Path mavenBuildProjectRoot, Collection<String> additionalArgs) {
        try {
            new Command(getBuildCmd(additionalArgs)).onDirectory(mavenBuildProjectRoot).runAndWait();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to build artifact: " + e.getMessage());
        }

        if (mode == Mode.JVM) {
            return findAndMoveJvmArtifact(mavenBuildProjectRoot).map(Path::of);
        } else {
            return findNativeExecutable(mavenBuildProjectRoot);
        }
    }

    private Optional<String> findAndMoveJvmArtifact(Path mavenBuildProjectRoot) {
        return findJvmArtifact(mavenBuildProjectRoot.resolve(TARGET))
                .map(jarPathStr -> {
                    // moves to permanent location when custom build not required
                    if (!isCustomBuildRequired()) {
                        var testProjectTargetQuarkusApp = Path.of(TARGET).resolve(QUARKUS_APP);
                        if (!Files.exists(testProjectTargetQuarkusApp)) {
                            var jarPath = Path.of(jarPathStr);
                            var quarkusAppFolder = jarPath.getParent();
                            FileUtils.copyDirectoryTo(quarkusAppFolder, testProjectTargetQuarkusApp);

                            // use moved artifact mainly so that we know it works
                            var jarName = jarPath.toFile().getName();
                            return testProjectTargetQuarkusApp.resolve(jarName).toAbsolutePath().toString();
                        }
                    }
                    return jarPathStr;
                });
    }

    static Optional<String> findJvmArtifact(Path targetFolder) {
        return findTargetFile(targetFolder, JVM_RUNNER)
                .or(() -> findTargetFile(targetFolder.resolve(QUARKUS_APP), QUARKUS_RUN));
    }

    private Optional<Path> findNativeExecutable(Path mavenBuildProjectRoot) {
        return findTargetFile(mavenBuildProjectRoot.resolve(TARGET), nativeRunnerName())
                .map(Path::of)
                .flatMap(this::moveToPermanentLocation);
    }

    /**
     * Moves built native executable from application folder target file to target folder for local artifacts.
     * This makes executable re-usable when flaky tests are re-run as app folder is deleted when the test is finished.
     */
    private Optional<Path> moveToPermanentLocation(Path tempNativeExecutablePath) {
        // find permanent re-usable location of our native executable
        final Path permanentNativeExecutablePath;
        // runtime properties provided at build time are currently available during the build time and so are custom props
        // therefore we can't allow re-using of native executable with application specific properties (e.g. "withProperty")
        resourceBuilder.createSnapshotOfBuildProperties();
        if (isCustomBuildRequired()) {
            String uniqueAppName = getUniqueAppName(appFolder);
            Path customExecutableTargetDir = targetFolderForLocalArtifacts.resolve(CUSTOM_RUNNER_DIR).resolve(uniqueAppName);
            customExecutableTargetDir.toFile().mkdirs();
            permanentNativeExecutablePath = customExecutableTargetDir.resolve(uniqueAppName + nativeRunnerName());
        } else {
            permanentNativeExecutablePath = targetFolderForLocalArtifacts.resolve("quarkus" + nativeRunnerName());
        }

        // delete existing executable if exists
        if (Files.exists(permanentNativeExecutablePath)) {
            FileUtils.deleteFile(permanentNativeExecutablePath.toFile());
        }

        // move executable to the permanent location
        final boolean moved = tempNativeExecutablePath.toFile().renameTo(permanentNativeExecutablePath.toFile());
        if (!moved) {
            throw new IllegalStateException("Failed to move native executable from '" + tempNativeExecutablePath.toFile()
                    + "' to '" + tempNativeExecutablePath.toFile() + "'");
        }
        return Optional.of(permanentNativeExecutablePath);
    }

    private boolean isCustomBuildRequired() {
        boolean customBuildRequiredImplicitly = resourceBuilder.requiresCustomBuild() || !forcedDependencies.isEmpty()
                || !requiredDependencies.isEmpty() || resourceBuilder.areApplicationPropertiesEnhanced();
        boolean customBuildRequiredExplicitly = TestExecutionProperties.isCustomBuildRequired(resourceBuilder.getContext());
        return customBuildRequiredExplicitly || customBuildRequiredImplicitly;
    }

    private String[] getBuildCmd(Collection<String> additionalArgs) {
        Stream<String> cmdStream = Stream.of(mvnCmd(), "-B", "--no-transfer-progress", "clean", "install");
        if (mode == Mode.NATIVE) {
            cmdStream = Stream.concat(cmdStream, Stream.of("-Dnative"));
        }
        cmdStream = Stream.concat(cmdStream, Stream.of("-DskipTests", "-DskipITs", "-Dcheckstyle.skip"));

        if (!isS2iScenario()) {
            cmdStream = Stream.concat(cmdStream, Stream.of(
                    toMvnSystemProperty(PLATFORM_VERSION.getPropertyKey(), getVersion()),
                    toMvnSystemProperty(PLATFORM_GROUP_ID.getPropertyKey(), PLATFORM_GROUP_ID.get()),
                    toMvnSystemProperty(PLUGIN_VERSION.getPropertyKey(), getPluginVersion())));
        }

        // Need to add local maven repo due to differences in `getCmdLineBuildArgs` as by default it's not picked on Windows
        if (OS.WINDOWS.isCurrent() && System.getProperty(MVN_REPOSITORY_LOCAL) != null) {
            cmdStream = Stream.concat(cmdStream,
                    Stream.of(toMvnSystemProperty(MVN_REPOSITORY_LOCAL, System.getProperty(MVN_REPOSITORY_LOCAL))));
        }
        var cmdLineBuildArgs = getCmdLineBuildArgs();
        if (!cmdLineBuildArgs.isEmpty()) {
            cmdStream = Stream.concat(cmdStream, cmdLineBuildArgs.stream());
        }

        cmdStream = Stream.concat(cmdStream, additionalArgs.stream());

        return cmdStream.toArray(String[]::new);
    }

    private void addEnhancedAppPropsAndMetaInf(Path mavenBuildProjectRoot) {
        final Set<Path> appDirContent;
        try (Stream<Path> pathStream = Files.list(appFolder)) {
            String mvnBuildProjectRootName = mavenBuildProjectRoot.toFile().getName();
            // app properties and every other file of merged main and test resources dirs
            appDirContent = pathStream
                    .filter(p -> !mvnBuildProjectRootName.equals(p.toFile().getName()))
                    .collect(toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path resourcesDir = mavenBuildProjectRoot.resolve("src").resolve("main").resolve("resources");
        FileUtils.recreateDirectory(resourcesDir);
        appDirContent.forEach(path -> {
            File file = path.toFile();
            if (file.isDirectory()) {
                FileUtils.copyDirectoryTo(path, resourcesDir.resolve(file.getName()));
            } else {
                FileUtils.copyFileTo(file, resourcesDir);
            }
        });
    }

    private void addAppClassesToSrcMain(Path mavenBuildProjectRoot) {
        // copy original project root src directory to new project root
        Path mainSourceDir = Path.of("src").resolve("main");
        if (!Files.exists(mainSourceDir)) {
            mainSourceDir.toFile().mkdirs();
        }

        Path newMainSourceDir = mavenBuildProjectRoot.resolve("src").resolve("main");
        // it's important to copy only src/main for when we remove non-application classes
        // compilation of test dir could lead to error due to missing classes
        FileUtils.copyDirectoryTo(mainSourceDir, newMainSourceDir);
        Path srcMainJava = newMainSourceDir.resolve("java");

        if (!buildWithAllClasses) {
            if (Files.exists(srcMainJava)) {
                // find non-application classes
                final Set<Path> nonAppClassesPaths;
                try (Stream<Path> stream = Files.walk(srcMainJava)) {
                    nonAppClassesPaths = stream
                            .filter(path -> path.toString().endsWith(JAVA_SUFFIX))
                            .filter(this::isNotAppClass)
                            .collect(toSet());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // delete non-application classes
                if (!nonAppClassesPaths.isEmpty()) {
                    for (Path nonAppClassPath : nonAppClassesPaths) {
                        FileUtils.deletePath(nonAppClassPath);
                    }
                }
            } else {
                // there are no app classes in src/main/java, so let's create the directory for test app classes
                srcMainJava.toFile().mkdirs();
            }

            // add app classes from src/test/java
            // app classes are 'merged' (app classes from both 'src/main/java' and 'src/test/java' are kept)

            // 1. find test app classes
            Path testSourceDir = Path.of("src").resolve("test").resolve("java");
            final Set<Path> testDirAppClassesPaths;
            try (Stream<Path> stream = Files.walk(testSourceDir)) {
                testDirAppClassesPaths = stream
                        .filter(path -> path.toString().endsWith(JAVA_SUFFIX))
                        .filter(this::isAppClass)
                        .collect(toSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 2. copy them to the new source dir
            if (!testDirAppClassesPaths.isEmpty()) {
                String normalizedTestSourceDirPath = testSourceDir.toAbsolutePath().normalize().toString();
                if (!normalizedTestSourceDirPath.endsWith(SLASH)) {
                    // this prevents situation when test app file sub-path starts with a '/'
                    normalizedTestSourceDirPath += SLASH;
                }
                for (Path appFilePath : testDirAppClassesPaths) {
                    String normalizedAppFileDirPath = appFilePath.getParent().toAbsolutePath().normalize().toString();
                    if (!normalizedAppFileDirPath.startsWith(normalizedTestSourceDirPath)) {
                        throw new IllegalStateException("Merging algorithm doesn't work correctly. "
                                + "Java test file '" + normalizedAppFileDirPath + "' does not belong to the '"
                                + normalizedTestSourceDirPath + "' folder.");
                    }
                    String javaDirSubPath = normalizedAppFileDirPath.substring(normalizedTestSourceDirPath.length());
                    Path newDir = srcMainJava.resolve(javaDirSubPath);
                    // create dir if it doesn't exist
                    newDir.toFile().mkdirs();
                    FileUtils.copyFileTo(appFilePath.toFile(), newDir);
                }
            }
        }
    }

    private boolean isNotAppClass(Path path) {
        String normalizedPath = ClassPathUtils.normalizeClassName(path.toString(), JAVA_SUFFIX);
        return appClassNames.stream().noneMatch(normalizedPath::endsWith);
    }

    private boolean isAppClass(Path path) {
        String normalizedPath = ClassPathUtils.normalizeClassName(path.toString(), JAVA_SUFFIX);
        return appClassNames.stream().anyMatch(normalizedPath::endsWith);
    }

    private void addForcedDependenciesToNewPom(Document pomDocument, Node projectElement) {
        addDependenciesToNewPom(pomDocument, projectElement, forcedDependencies);
    }

    private static void addDependenciesToNewPom(Document pomDocument, Node projectElement, List<Dependency> dependencies) {
        var projectChildElements = projectElement.getChildNodes();
        for (int i = 0; i < projectChildElements.getLength(); i++) {
            Node childNode = projectChildElements.item(i);
            if ("dependencies".equals(childNode.getNodeName())) {
                for (Dependency dependency : dependencies) {
                    // <dependency>
                    Element newDependency = pomDocument.createElement("dependency");
                    Element artifactId = pomDocument.createElement("artifactId");
                    // <artifactId>
                    artifactId.setTextContent(dependency.artifactId());
                    newDependency.appendChild(artifactId);
                    // <groupId>
                    Element groupId = pomDocument.createElement("groupId");
                    if (dependency.groupId().isEmpty()) {
                        groupId.setTextContent("io.quarkus");
                    } else {
                        groupId.setTextContent(dependency.groupId());
                    }
                    newDependency.appendChild(groupId);
                    // <version>
                    if (dependency.version() != null && !dependency.version().isEmpty()) {
                        Element version = pomDocument.createElement("version");
                        version.setTextContent(dependency.version());
                        newDependency.appendChild(version);
                    }
                    childNode.appendChild(newDependency);
                }
                break;
            }
        }
    }

    private List<String> getCmdLineBuildArgs() {
        // we don't look for command line args on Windows as arguments are not accessible via the 'ProcessHandle' API
        // TODO: if we ever extend native executable coverage on Windows, we must find a way how to access only original args
        return OS.WINDOWS.isCurrent() ? List.copyOf(resourceBuilder.getBuildPropertiesSetAsSystemProperties())
                : findMavenCommandLineArgs();
    }

    private static Document getDocument(Path newPom) {
        var builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);
        builderFactory.setIgnoringComments(true);
        builderFactory.setExpandEntityReferences(false);
        try {
            return builderFactory.newDocumentBuilder().parse(newPom.toFile());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to parse '" + newPom + "' document: " + e.getMessage());
        }
    }

    private static void setProjectBuildNameToApp(Node projectElement, Document pomDocument) {
        var projectChildElements = projectElement.getChildNodes();
        Node buildNode = null;
        for (int i = 0; i < projectChildElements.getLength(); i++) {
            Node childNode = projectChildElements.item(i);
            if ("build".equals(childNode.getNodeName())) {
                buildNode = childNode;
                break;
            }
        }
        if (buildNode == null) {
            buildNode = pomDocument.createElement("build");
            projectElement.appendChild(buildNode);
        }
        Node finalNameNode = null;
        var buildChildElements = buildNode.getChildNodes();
        for (int i = 0; i < buildChildElements.getLength(); i++) {
            Node childNode = buildChildElements.item(i);
            if ("finalName".equals(childNode.getNodeName())) {
                finalNameNode = childNode;
                break;
            }
        }
        if (finalNameNode == null) {
            finalNameNode = pomDocument.createElement("finalName");
            buildNode.appendChild(finalNameNode);
        }
        finalNameNode.setTextContent("app");
    }

    private static Path findMavenWrapper() {
        return findMavenWrapper(Path.of("").toAbsolutePath());
    }

    private static Path findMavenWrapper(Path dir) {
        if (dir == null) {
            return null;
        }
        Path mvnWrapper = dir.resolve(mvnWrapperName());
        if (Files.exists(mvnWrapper)) {
            return mvnWrapper;
        } else {
            return findMavenWrapper(dir.getParent());
        }
    }

    private static String mvnWrapperName() {
        return OS.current() == OS.WINDOWS ? "mvnw.exe" : "mvnw";
    }

    private static String mvnCmd() {
        try {
            new Command("mvn", "--version").runAndWait();
        } catch (IOException | InterruptedException e) {
            Path mvnWrapperPath = findMavenWrapper();
            if (mvnWrapperPath != null) {
                return mvnWrapperPath.toString();
            }
            throw new IllegalStateException(
                    "Project either needs to contain Maven wrapper or runner must have installed Maven");
        }
        return "mvn";
    }

    static Optional<String> findNativeBuildExecutable(Path targetFolder, boolean customBuildRequired, Path appFolder) {
        if (customBuildRequired) {
            final String uniqueAppName = getUniqueAppName(appFolder);
            final String customExecutableName = uniqueAppName + nativeRunnerName();
            return findTargetFile(targetFolder.resolve(CUSTOM_RUNNER_DIR).resolve(uniqueAppName),
                    customExecutableName::equalsIgnoreCase);
        }
        return findTargetFile(targetFolder, nativeRunnerName());
    }

    private static String getUniqueAppName(Path appFolder) {
        // test class + app name
        return appFolder.getParent().toFile().getName() + "-" + appFolder.toFile().getName();
    }

    private static String nativeRunnerName() {
        if (org.junit.jupiter.api.condition.OS.WINDOWS.isCurrentOs()) {
            return NATIVE_RUNNER + EXE;
        } else {
            return NATIVE_RUNNER;
        }
    }

    private enum Mode {
        JVM,
        NATIVE
    }

}
