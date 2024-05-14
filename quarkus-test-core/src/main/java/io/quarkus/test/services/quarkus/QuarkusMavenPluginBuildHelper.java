package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder.EXE;
import static io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder.NATIVE_RUNNER;
import static io.quarkus.test.services.quarkus.ProdQuarkusApplicationManagedResourceBuilder.TARGET;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_GROUP_ID;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_VERSION;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLUGIN_VERSION;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.getPluginVersion;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.getVersion;
import static io.quarkus.test.utils.FileUtils.findTargetFile;
import static io.quarkus.test.utils.PropertiesUtils.toMvnSystemProperty;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.Command;
import io.quarkus.test.utils.FileUtils;
import io.smallrye.common.os.OS;

class QuarkusMavenPluginBuildHelper {

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
    private final Set<String> cmdLineBuildArgs;
    private final boolean buildWithAllClasses;
    private final boolean customBuildRequired;
    private final Path targetFolderForLocalArtifacts;
    private final QuarkusApplicationManagedResourceBuilder resourceBuilder;

    QuarkusMavenPluginBuildHelper(QuarkusApplicationManagedResourceBuilder resourceBuilder,
            Path targetFolderForLocalArtifacts) {
        this.resourceBuilder = resourceBuilder;
        resourceBuilder.createSnapshotOfBuildProperties();
        this.appFolder = resourceBuilder.getApplicationFolder();
        this.appClassNames = Arrays.stream(resourceBuilder.getAppClasses()).map(Class::getName).collect(toUnmodifiableSet());
        this.buildWithAllClasses = resourceBuilder.isBuildWithAllClasses();
        // runtime properties provided at build time are currently available during the build time and so are custom props
        // therefore we can't allow re-using of native executable with application specific properties (e.g. "withProperty")
        this.customBuildRequired = resourceBuilder.requiresCustomBuild() || resourceBuilder.hasAppSpecificConfigProperties();
        this.targetFolderForLocalArtifacts = targetFolderForLocalArtifacts;

        this.forcedDependencies = List.copyOf(resourceBuilder.getForcedDependencies());
        // we don't look for command line args on Windows as arguments are not accessible via the 'ProcessHandle' API
        // TODO: if we ever extend native executable coverage on Windows, we must find a way how to access only original args
        this.cmdLineBuildArgs = OS.WINDOWS.isCurrent() ? Set.copyOf(resourceBuilder.getBuildPropertiesSetAsSystemProperties())
                : findMavenCommandLineArgs();
    }

    private static Set<String> findMavenCommandLineArgs() {
        Set<String> mvnArgs = new HashSet<>();
        ProcessHandle processHandle = ProcessHandle.current();
        do {
            if (processHandle.info().arguments().isPresent()) {
                String[] args = processHandle.info().arguments().get();
                boolean keepArgs = false;
                for (String arg : args) {
                    if (arg == null) {
                        // this is probably not necessary, but let's stay on the safe side
                        continue;
                    }
                    if (keepArgs && arg.startsWith("-D")) {
                        mvnArgs.add(arg);
                    } else if (LAUNCHER.equals(arg) || FAILSAFE_BUILD_LIFECYCLE_PHASES.contains(arg)) {
                        // order is important here - we only want to propagate mvn args
                        keepArgs = true;
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

    void prepareApplicationFolder() {
        prepareMavenProject(appFolder);
        resourceBuilder.copyResourcesToAppFolder();
    }

    private Path prepareMavenProject(Path mavenBuildProjectRoot) {
        // create new project root
        FileUtils.recreateDirectory(mavenBuildProjectRoot);

        // add pom.xml copy to new project root
        FileUtils.copyFileTo(Path.of(POM_XML).toFile(), mavenBuildProjectRoot);
        Path newPom = mavenBuildProjectRoot.resolve(POM_XML);

        // adjust pom.xml
        Document pomDocument = getDocument(newPom);
        Node projectElement = pomDocument.getElementsByTagName("project").item(0);
        setCorrectProjectParentRelativePath(projectElement, mavenBuildProjectRoot);
        if (OS.WINDOWS.isCurrent()) {
            // it's necessary to keep executable name small as I experienced LNK1104 error on Windows from long path
            // see also: https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation
            setProjectBuildNameToApp(projectElement, pomDocument);
        }
        // add forced dependencies
        if (!forcedDependencies.isEmpty()) {
            addForcedDependenciesToNewPom(pomDocument, projectElement);
        }
        // update pom.xml file
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(pomDocument), new StreamResult(newPom.toFile()));
        } catch (TransformerException e) {
            throw new RuntimeException("Failed to persist enhanced POM file: " + e.getMessage());
        }

        // add application classes to src/main/..
        addAppClassesToSrcMain(mavenBuildProjectRoot);

        // add enhanced application.properties and META-INF to the new project
        addEnhancedAppPropsAndMetaInf(mavenBuildProjectRoot);

        return mavenBuildProjectRoot;
    }

    Optional<Path> buildNativeExecutable() {
        Objects.requireNonNull(targetFolderForLocalArtifacts);
        var mavenBuildProjectRoot = prepareMavenProject(appFolder.resolve("mvn-build"));

        // build artifact with Quarkus Maven Plugin
        try {
            new Command(getBuildNativeExecutableCmd()).onDirectory(mavenBuildProjectRoot).runAndWait();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to build native executable: " + e.getMessage());
        }

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
        if (customBuildRequired) {
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

    private String[] getBuildNativeExecutableCmd() {
        Stream<String> cmdStream = Stream.of(mvnCmd(), "clean", "install", "-Dquarkus.build.skip=false",
                "-Dnative", "-DskipTests", "-DskipITs", "-Dcheckstyle.skip",
                toMvnSystemProperty(PLATFORM_VERSION.getPropertyKey(), getVersion()),
                toMvnSystemProperty(PLATFORM_GROUP_ID.getPropertyKey(), PLATFORM_GROUP_ID.get()),
                toMvnSystemProperty(PLUGIN_VERSION.getPropertyKey(), getPluginVersion()));
        if (!cmdLineBuildArgs.isEmpty()) {
            cmdStream = Stream.concat(cmdStream, cmdLineBuildArgs.stream());
        }
        return cmdStream.toArray(String[]::new);
    }

    private void setCorrectProjectParentRelativePath(Node projectElement, Path newPomDir) {
        // this only works for parent POM located on the file system
        var projectChildElements = projectElement.getChildNodes();
        for (int i = 0; i < projectChildElements.getLength(); i++) {
            Node projectChildNode = projectChildElements.item(i);
            if ("parent".equals(projectChildNode.getNodeName())) {
                var parentChildNodes = projectChildNode.getChildNodes();
                for (int j = 0; j < parentChildNodes.getLength(); j++) {
                    var parentChildNode = parentChildNodes.item(j);
                    if ("relativePath".equals(parentChildNode.getNodeName())) {
                        String relativeDirPath = parentChildNode.getTextContent();
                        if (relativeDirPath != null && !relativeDirPath.isBlank()) {
                            relativeDirPath = relativeDirPath.trim();
                            if (relativeDirPath.endsWith(POM_XML)) {
                                // point to dir and not a POM file
                                relativeDirPath = relativeDirPath.substring(0, relativeDirPath.length() - POM_XML.length());
                            }
                        } else {
                            // set default relative path
                            relativeDirPath = "..";
                        }
                        Path parentPomDirPath = Path.of(relativeDirPath).toAbsolutePath();
                        String newPomToParentDirPath = parentPomDirPath.relativize(newPomDir.toAbsolutePath()).toString();

                        // one/two/three => ../../..
                        String splitBySeparator = Pattern.quote(File.separator);
                        String newRelativePath = Arrays.stream(newPomToParentDirPath.split(splitBySeparator))
                                .map(w -> "..")
                                .collect(Collectors.joining(File.separator));

                        parentChildNode.setTextContent(newRelativePath + File.separator + POM_XML);
                        break;
                    }
                }
                break;
            }
        }
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
                if (!normalizedTestSourceDirPath.endsWith("/")) {
                    // this prevents situation when test app file sub-path starts with a '/'
                    normalizedTestSourceDirPath += "/";
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
        var projectChildElements = projectElement.getChildNodes();
        for (int i = 0; i < projectChildElements.getLength(); i++) {
            Node childNode = projectChildElements.item(i);
            if ("dependencies".equals(childNode.getNodeName())) {
                for (Dependency forcedDependency : forcedDependencies) {
                    // <dependency>
                    Element newDependency = pomDocument.createElement("dependency");
                    Element artifactId = pomDocument.createElement("artifactId");
                    // <artifactId>
                    artifactId.setTextContent(forcedDependency.getArtifactId());
                    newDependency.appendChild(artifactId);
                    // <groupId>
                    Element groupId = pomDocument.createElement("groupId");
                    if (forcedDependency.getGroupId().isEmpty()) {
                        groupId.setTextContent("io.quarkus");
                    } else {
                        groupId.setTextContent(forcedDependency.getGroupId());
                    }
                    newDependency.appendChild(groupId);
                    // <version>
                    if (!forcedDependency.getVersion().isEmpty()) {
                        Element version = pomDocument.createElement("version");
                        version.setTextContent(forcedDependency.getVersion());
                        newDependency.appendChild(version);
                    }
                    childNode.appendChild(newDependency);
                }
                break;
            }
        }
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
}
