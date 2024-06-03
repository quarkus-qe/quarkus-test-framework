package io.quarkus.docs.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItemScanner;
import io.quarkus.annotation.processor.generate_doc.ConfigPhase;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class DeploymentConfigDetector {

    public static Artifact toAetherArtifact(ArtifactCoords coords) {
        return new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType(),
                coords.getVersion());
    }

    public static void main(String[] args) throws Exception {
        String jsonCatalog = "../devtools/bom-descriptor-json/target/quarkus-bom-quarkus-platform-descriptor-999-SNAPSHOT-999-SNAPSHOT.json";

        // This is where we produce the entire list of extensions
        File jsonFile = new File(jsonCatalog);
        if (!jsonFile.exists()) {
            throw new RuntimeException(
                    "Could not detect deployment build-time config because extension catalog file is missing: " + jsonFile);
        }
        MavenArtifactResolver resolver = MavenArtifactResolver.builder().setWorkspaceDiscovery(false).build();

        final ExtensionCatalog extensionJson = ExtensionCatalog.fromFile(jsonFile.toPath());

        // now get all the listed extension jars via Maven
        List<ArtifactRequest> requests = new ArrayList<>(extensionJson.getExtensions().size());
        Map<String, Extension> extensionsByGav = new HashMap<>();
        Map<String, Extension> extensionsByConfigRoots = new HashMap<>();
        for (Extension extension : extensionJson.getExtensions()) {
            final Artifact artifact = toAetherArtifact(extension.getArtifact());
            requests.add(new ArtifactRequest().setArtifact(artifact));
            // record the extension for this GAV
            extensionsByGav.put(artifact.getGroupId() + ":" + artifact.getArtifactId(), extension);
        }

        // examine all the extension jars
        List<ArtifactRequest> deploymentRequests = new ArrayList<>(extensionJson.getExtensions().size());
        for (ArtifactResult result : resolver.resolve(requests)) {
            final Artifact artifact = result.getArtifact();
            // which extension was this for?
            Extension extension = extensionsByGav.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
            try (ZipFile zf = new ZipFile(artifact.getFile())) {
                // collect all its config roots
                if (artifact.getArtifactId().endsWith("-deployment")) {
                    collectConfigRoots(zf, extension, extensionsByConfigRoots);
                }

                // see if it has a deployment artifact we need to load
                ZipEntry entry = zf.getEntry("META-INF/quarkus-extension.properties");
                if (entry != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(zf.getInputStream(entry), StandardCharsets.UTF_8))) {
                        Properties properties = new Properties();
                        properties.load(reader);
                        final String deploymentCoords = (String) properties.get("deployment-artifact");
                        // if it has one, load it
                        if (deploymentCoords != null) {
                            final Artifact deploymentArtifact = toAetherArtifact(ArtifactCoords.fromString(deploymentCoords));
                            deploymentRequests.add(new ArtifactRequest().setArtifact(deploymentArtifact));
                            // tie this artifact to its extension
                            extensionsByGav.put(deploymentArtifact.getGroupId() + ":" + deploymentArtifact.getArtifactId(),
                                    extension);
                        }
                    }
                }
            }
        }

        // now examine all the extension deployment jars
        for (ArtifactResult result : resolver.resolve(deploymentRequests)) {
            final Artifact artifact = result.getArtifact();
            // which extension was this for?
            Extension extension = extensionsByGav.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
            try (ZipFile zf = new ZipFile(artifact.getFile())) {
                // collect all its config roots
                if (artifact.getArtifactId().endsWith("-deployment")) {
                    collectConfigRoots(zf, extension, extensionsByConfigRoots);
                }
            }
        }

        // load all the config items per config root
        final ConfigDocItemScanner configDocItemScanner = new ConfigDocItemScanner();
        final Map<String, List<ConfigDocItem>> docItemsByConfigRoots = configDocItemScanner
                .loadAllExtensionsConfigurationItems();

        // Temporary fix for https://github.com/quarkusio/quarkus/issues/5214 until we figure out how to fix it
        Extension openApi = extensionsByGav.get("io.quarkus:quarkus-smallrye-openapi");
        if (openApi != null) {
            extensionsByConfigRoots.put("io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig", openApi);
        }

        var buildTimeConfigKeys = new HashSet<String>();
        for (Entry<String, Extension> entry : extensionsByConfigRoots.entrySet()) {
            List<ConfigDocItem> items = docItemsByConfigRoots.get(entry.getKey());
            if (items != null) {
                for (ConfigDocItem item : items) {
                    if (item.getConfigPhase() == ConfigPhase.BUILD_TIME && item.getConfigDocKey() != null) {
                        // this won't match named properties
                        if (item.getConfigDocKey().getKey() != null) {
                            buildTimeConfigKeys.add(item.getConfigDocKey().getKey());
                        }
                    }
                }
            }
        }

        if (!buildTimeConfigKeys.isEmpty()) {
            Files.write(Path.of("target").resolve("deployment-build-props.txt"), buildTimeConfigKeys);
        }
    }

    private static void collectConfigRoots(ZipFile zf, Extension extension, Map<String, Extension> extensionsByConfigRoots)
            throws IOException {
        ZipEntry entry = zf.getEntry("META-INF/quarkus-config-roots.list");
        if (entry != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zf.getInputStream(entry), StandardCharsets.UTF_8))) {
                // make sure we turn $ into . because javadoc-scanned class names are dot-separated
                reader.lines().map(String::trim).filter(str -> !str.isEmpty()).map(str -> str.replace('$', '.'))
                        .forEach(klass -> extensionsByConfigRoots.put(klass, extension));
            }
        }
    }
}
