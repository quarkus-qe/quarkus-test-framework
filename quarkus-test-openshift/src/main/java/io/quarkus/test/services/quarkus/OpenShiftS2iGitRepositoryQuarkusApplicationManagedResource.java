package io.quarkus.test.services.quarkus;

import static io.quarkus.test.configuration.Configuration.Property.S2I_BASE_NATIVE_IMAGE;
import static io.quarkus.test.configuration.Configuration.Property.S2I_MAVEN_RELEASES_REPOSITORY;
import static io.quarkus.test.configuration.Configuration.Property.S2I_MAVEN_REMOTE_REPOSITORY;
import static io.quarkus.test.configuration.Configuration.Property.S2I_MAVEN_REMOTE_REPOSITORY_PASSWORD;
import static io.quarkus.test.configuration.Configuration.Property.S2I_MAVEN_REMOTE_REPOSITORY_USERNAME;
import static io.quarkus.test.configuration.Configuration.Property.S2I_MAVEN_SNAPSHOTS_REPOSITORY;
import static io.quarkus.test.configuration.Configuration.Property.S2I_REPLACE_CA_CERTS;
import static io.quarkus.test.services.quarkus.GitRepositoryQuarkusApplicationManagedResourceBuilder.QUARKUS_PLATFORM_GROUP_ID_PROPERTY;
import static io.quarkus.test.services.quarkus.GitRepositoryQuarkusApplicationManagedResourceBuilder.QUARKUS_PLATFORM_VERSION_PROPERTY;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_GROUP_ID;
import static io.quarkus.test.services.quarkus.model.QuarkusProperties.QUARKUS_JVM_S2I;
import static java.util.regex.Pattern.quote;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.Log;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshotCondition;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;

public class OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource
        extends TemplateOpenShiftQuarkusApplicationManagedResource<GitRepositoryQuarkusApplicationManagedResourceBuilder> {

    private static final String QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME = "/quarkus-s2i-source-build-template.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME = "settings-mvn.yml";
    private static final String QUARKUS_SOURCE_S2I_SETTINGS_MVN_AUTH_FILENAME = "settings-mvn-auth.yml";
    private static final String INTERNAL_MAVEN_REPOSITORY_PROPERTY = "${internal.s2i.maven.remote.repository}";

    // Repository block placeholders resolved inside settings-mvn-auth.yml replaced with XML or empty string
    private static final String CENTRAL_MIRROR_BLOCK_PLACEHOLDER = "${central.mirror.block}";
    private static final String SERVERS_BLOCK_PLACEHOLDER = "${servers.block}";
    private static final String SNAPSHOTS_REPOSITORY_BLOCK_PLACEHOLDER = "${snapshots.repository.block}";
    private static final String SNAPSHOTS_PLUGIN_REPOSITORY_BLOCK_PLACEHOLDER = "${snapshots.pluginRepository.block}";

    private static final String DEFAULT_MAVEN_CENTRAL_MIRROR = "https://maven-central.storage-download.googleapis.com/maven2/";

    // Mirror XML for central used for releases repo or Google default
    private static final String CENTRAL_MIRROR_XML = "<mirror>\n"
            + "                <id>%s</id>\n"
            + "                <mirrorOf>central</mirrorOf>\n"
            + "                <name>%s</name>\n"
            + "                <url>%s</url>\n"
            + "                <blocked>false</blocked>\n"
            + "            </mirror>";

    // Snapshot repository/pluginRepository XML blocks
    private static final String SNAPSHOTS_REPOSITORY_XML = "<repository>\n"
            + "                        <id>internal.s2i.maven.snapshots.repository</id>\n"
            + "                        <url>%s</url>\n"
            + "                        <releases><enabled>false</enabled></releases>\n"
            + "                        <snapshots><enabled>true</enabled></snapshots>\n"
            + "                    </repository>";
    private static final String SNAPSHOTS_PLUGIN_REPOSITORY_XML = "<pluginRepository>\n"
            + "                        <id>internal.s2i.maven.snapshots.repository</id>\n"
            + "                        <url>%s</url>\n"
            + "                        <releases><enabled>false</enabled></releases>\n"
            + "                        <snapshots><enabled>true</enabled></snapshots>\n"
            + "                    </pluginRepository>";

    // Servers block - only emitted when both username and password are set
    private static final String SERVERS_BLOCK_XML = "<server>\n"
            + "                <id>internal.s2i.maven.releases.repository</id>\n"
            + "                <username>%s</username>\n"
            + "                <password>%s</password>\n"
            + "            </server>\n"
            + "            <server>\n"
            + "                <id>internal.s2i.maven.snapshots.repository</id>\n"
            + "                <username>%s</username>\n"
            + "                <password>%s</password>\n"
            + "            </server>";

    private static final PropertyLookup MAVEN_REMOTE_REPOSITORY = new PropertyLookup(
            S2I_MAVEN_REMOTE_REPOSITORY.getName());
    private static final PropertyLookup MAVEN_RELEASES_REPOSITORY = new PropertyLookup(
            S2I_MAVEN_RELEASES_REPOSITORY.getName());
    private static final PropertyLookup MAVEN_SNAPSHOTS_REPOSITORY = new PropertyLookup(
            S2I_MAVEN_SNAPSHOTS_REPOSITORY.getName());
    private static final PropertyLookup MAVEN_REMOTE_REPOSITORY_USERNAME = new PropertyLookup(
            S2I_MAVEN_REMOTE_REPOSITORY_USERNAME.getName());
    private static final PropertyLookup MAVEN_REMOTE_REPOSITORY_PASSWORD = new PropertyLookup(
            S2I_MAVEN_REMOTE_REPOSITORY_PASSWORD.getName());
    private static final PropertyLookup REPLACE_JAVA_CA_CERTS = new PropertyLookup(S2I_REPLACE_CA_CERTS.getName());
    private static final String ETC_PKI_JAVA_CONFIG_MAP_NAME = "etc-pki-java";
    private static final PropertyLookup QUARKUS_NATIVE_S2I_FROM_SRC = new PropertyLookup(
            S2I_BASE_NATIVE_IMAGE.getName(),
            "quay.io/quarkus/ubi9-quarkus-graalvmce-s2i:jdk-21");
    private static final String QUARKUS_SOURCE_S2I_NATIVE_BUILD_PROPERTIES = "-Dquarkus.native.native-image-xmx=5g";

    private final GitRepositoryQuarkusApplicationManagedResourceBuilder model;

    public OpenShiftS2iGitRepositoryQuarkusApplicationManagedResource(
            GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void validate() {
        super.validate();
        if (model.isDevMode()) {
            Assertions.fail("DEV mode is not supported when using GIT repositories on OpenShift deployments");
        }
        boolean snapshotRepoConfigured = StringUtils.isNotEmpty(MAVEN_SNAPSHOTS_REPOSITORY.get(model.getContext()))
                || StringUtils.isNotEmpty(MAVEN_REMOTE_REPOSITORY.get(model.getContext()));
        if (DisabledOnQuarkusSnapshotCondition.isQuarkusSnapshotVersion() && !snapshotRepoConfigured) {
            Assertions.fail("s2i can't use the Quarkus 999-SNAPSHOT version if no Maven snapshot repository has been provided");
        }
    }

    @Override
    protected String getDefaultTemplate() {
        return QUARKUS_SOURCE_S2I_BUILD_TEMPLATE_FILENAME;
    }

    /**
     * - Express parameters in S2I build application (resources/quarkus-s2i-source-build-template.yml).
     * - Add environment variables to deployment config in S2I build application
     * (resources/quarkus-s2i-source-build-template.yml).
     * - Enrich deployment config for purposes of the test suite in S2I build application
     * (resources/quarkus-s2i-source-build-template.yml).
     * - Apply the resulting OpenShift yml file.
     * - Wait for build config to have a complete build.
     * - Wait for deployment to be ready.
     */
    @Override
    protected void doInit() {
        createMavenSettings();
        super.doInit();
        client.followBuildConfigLogs(model.getContext().getName());
    }

    @Override
    protected boolean needsBuildArtifact() {
        return false;
    }

    protected String replaceDeploymentContent(String content) {
        String quarkusPlatformVersion = QuarkusProperties.getVersion();
        String quarkusS2iBaseImage = getQuarkusS2iBaseImage();
        String quarkusSourceS2iBuildNativeProperties = isNativeTest() ? QUARKUS_SOURCE_S2I_NATIVE_BUILD_PROPERTIES : "";
        String mavenArgs = model.getMavenArgsWithVersion();

        return content.replaceAll(quote("${APP_NAME}"), model.getContext().getOwner().getName())
                .replaceAll(quote("${QUARKUS_S2I_BUILDER_IMAGE}"), quarkusS2iBaseImage)
                .replaceAll(quote("${GIT_URI}"), model.getGitRepository())
                .replaceAll(quote("${GIT_REF}"), model.getGitBranch())
                .replaceAll(quote("${CONTEXT_DIR}"), model.getContextDir())
                .replaceAll(quote("${GIT_MAVEN_ARGS}"), mavenArgs)
                .replaceAll(quote("${CURRENT_NAMESPACE}"), client.project())
                .replaceAll(quote("${QUARKUS_SOURCE_S2I_NATIVE_BUILD_PROPERTIES}"), quarkusSourceS2iBuildNativeProperties)
                .replaceAll(quote(QUARKUS_PLATFORM_GROUP_ID_PROPERTY), PLATFORM_GROUP_ID.get())
                .replaceAll(quote(QUARKUS_PLATFORM_VERSION_PROPERTY), quarkusPlatformVersion);
    }

    private String getQuarkusS2iBaseImage() {
        PropertyLookup s2iImageProperty = isNativeTest() ? QUARKUS_NATIVE_S2I_FROM_SRC : QUARKUS_JVM_S2I;
        return model.getContext().getOwner().getProperty(s2iImageProperty.getPropertyKey())
                .orElseGet(() -> s2iImageProperty.get(model.getContext()));
    }

    private void createMavenSettings() {
        String releasesRepo = MAVEN_RELEASES_REPOSITORY.get(model.getContext());
        String snapshotsRepo = MAVEN_SNAPSHOTS_REPOSITORY.get(model.getContext());
        boolean useSplitRepos = StringUtils.isNotEmpty(releasesRepo) || StringUtils.isNotEmpty(snapshotsRepo);

        boolean replaceJavaCaCerts;
        String content;

        if (useSplitRepos) {
            content = buildAuthSettingsContent(releasesRepo, snapshotsRepo);
            String repoForCaCerts = StringUtils.defaultIfEmpty(releasesRepo, snapshotsRepo);
            replaceJavaCaCerts = shouldReplaceJavaCaCerts(repoForCaCerts);
        } else {
            content = buildRemoteRepositorySettingsContent();
            String remoteRepo = MAVEN_REMOTE_REPOSITORY.get(model.getContext());
            replaceJavaCaCerts = StringUtils.isNotEmpty(remoteRepo) && shouldReplaceJavaCaCerts(remoteRepo);
        }

        prepareJavaCaCerts(replaceJavaCaCerts);

        Path targetFile = model.getContext().getServiceFolder().resolve(QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME);
        FileUtils.copyContentTo(content, targetFile);
        client.apply(targetFile);
    }

    /**
     * Loads settings-mvn-auth.yml and resolves all placeholders.
     * The releases repository (or Google Maven Central as default) is wired as a mirror of central.
     * The snapshots repository, if configured, is added as a plain repository in the profile.
     * Credentials are injected only when both username and password are provided.
     */
    private String buildAuthSettingsContent(String releasesRepo, String snapshotsRepo) {
        String content = FileUtils.loadFile("/" + QUARKUS_SOURCE_S2I_SETTINGS_MVN_AUTH_FILENAME);

        // Central mirror: use configured releases repo, fall back to default (Google Maven Central)
        String centralMirrorUrl = StringUtils.defaultIfEmpty(releasesRepo, DEFAULT_MAVEN_CENTRAL_MIRROR);
        String centralMirrorId = StringUtils.isNotEmpty(releasesRepo)
                ? "internal.s2i.maven.releases.repository"
                : "google-maven-central";
        String centralMirrorName = StringUtils.isNotEmpty(releasesRepo)
                ? "Releases repository mirror of central"
                : "Google Maven Central mirror";
        content = content.replace(CENTRAL_MIRROR_BLOCK_PLACEHOLDER,
                String.format(CENTRAL_MIRROR_XML, centralMirrorId, centralMirrorName, centralMirrorUrl));

        // Snapshots repository only added when a URL is configured
        content = content.replace(SNAPSHOTS_REPOSITORY_BLOCK_PLACEHOLDER,
                StringUtils.isNotEmpty(snapshotsRepo)
                        ? String.format(SNAPSHOTS_REPOSITORY_XML, snapshotsRepo)
                        : "");
        content = content.replace(SNAPSHOTS_PLUGIN_REPOSITORY_BLOCK_PLACEHOLDER,
                StringUtils.isNotEmpty(snapshotsRepo)
                        ? String.format(SNAPSHOTS_PLUGIN_REPOSITORY_XML, snapshotsRepo)
                        : "");

        // Credentials: only inject servers block when both username and password are provided
        String username = MAVEN_REMOTE_REPOSITORY_USERNAME.get(model.getContext());
        String password = MAVEN_REMOTE_REPOSITORY_PASSWORD.get(model.getContext());
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            content = content.replace(SERVERS_BLOCK_PLACEHOLDER,
                    String.format(SERVERS_BLOCK_XML, username, password, username, password));
        } else {
            if (StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(password)) {
                Log.warn("Only one of s2i.maven.remote.repository.username / s2i.maven.remote.repository.password is set; "
                        + "both are required for authenticated access. Proceeding without authentication.");
            }
            content = content.replace(SERVERS_BLOCK_PLACEHOLDER, "");
        }

        return content;
    }

    /**
     * Builds settings.xml content for the single remote repository case (settings-mvn.yml).
     */
    private String buildRemoteRepositorySettingsContent() {
        String content = FileUtils.loadFile("/" + QUARKUS_SOURCE_S2I_SETTINGS_MVN_FILENAME);
        String remoteRepo = MAVEN_REMOTE_REPOSITORY.get(model.getContext());
        if (StringUtils.isNotEmpty(remoteRepo)) {
            content = content.replaceAll(quote(INTERNAL_MAVEN_REPOSITORY_PROPERTY), remoteRepo);
        }
        return content;
    }

    private void prepareJavaCaCerts(boolean replaceJavaCaCerts) {
        if (isDefaultTemplate() || templateInjectsEtcPkiJava()) {
            Path javaCaCertsPath = Path.of("/etc/pki/java/cacerts");
            if (replaceJavaCaCerts && Files.exists(javaCaCertsPath)) {
                // propagate java ca certs from executor machines so that secured communication
                // with remote repositories can use private certificate authority
                Log.info("Creating '%s' config map with 'cacerts' file".formatted(ETC_PKI_JAVA_CONFIG_MAP_NAME));
                client.createConfigMap(ETC_PKI_JAVA_CONFIG_MAP_NAME, javaCaCertsPath);
            } else {
                // build config doesn't support optional config mappings
                // this does not overwrite default ca certs
                Log.info("Creating empty '%s' config map".formatted(ETC_PKI_JAVA_CONFIG_MAP_NAME));
                client.createEmptyConfigMap(ETC_PKI_JAVA_CONFIG_MAP_NAME);
            }
        }
    }

    private boolean templateInjectsEtcPkiJava() {
        // our custom templates will need to use remote repository as well
        var template = FileUtils.loadFile(getTemplate());
        return template != null && template.contains(ETC_PKI_JAVA_CONFIG_MAP_NAME);
    }

    private boolean shouldReplaceJavaCaCerts(String remoteRepo) {
        String replaceJavaCaCertsAsString = REPLACE_JAVA_CA_CERTS.get(model.getContext());
        if (replaceJavaCaCertsAsString == null || replaceJavaCaCertsAsString.isEmpty()) {
            // property not set; by default recognize own repository so that we don't need to set it everywhere
            return remoteRepo.contains("eng.redhat.com") || remoteRepo.contains("engineering.redhat.com")
                    || remoteRepo.contains("corp.redhat.com");
        }
        return Boolean.parseBoolean(replaceJavaCaCertsAsString);
    }

    private boolean isDefaultTemplate() {
        return getDefaultTemplate().equals(getTemplate());
    }
}
