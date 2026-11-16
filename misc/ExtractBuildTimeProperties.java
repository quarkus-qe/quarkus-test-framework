package io.quarkus.docs.generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

public class ExtractBuildTimeProperties {

    public static void main(String[] args) throws IOException {
        Path repoRoot = Path.of("..").toAbsolutePath().normalize();
        Path outputFile = Path.of("deployment-build-props.txt").toAbsolutePath();

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectMapper jsonMapper = new ObjectMapper();
        Set<String> buildTimeProperties = new TreeSet<>();

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            List<Path> modelFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        String fullPath = p.toString().replace('\\', '/');
                        return fullPath.contains("/target/")
                                && (fileName.equals("quarkus-config-model.yaml")
                                        || fileName.equals("quarkus-config-model.json"));
                    })
                    .toList();

            if (modelFiles.isEmpty()) {
                System.err.println("No quarkus-config-model files found under " + repoRoot);
                System.err.println("First compile the repo using " +
                        "'mvn clean install -Dquickly -Dno-test-modules -Prelocations'");
                return;
            }

            for (Path modelFile : modelFiles) {
                ObjectMapper mapper = modelFile.toString().endsWith(".json") ? jsonMapper : yamlMapper;
                JsonNode rootNode = mapper.readTree(modelFile.toFile());
                collectBuildTimeProperties(rootNode, buildTimeProperties);
            }
        }

        Files.write(outputFile, buildTimeProperties);
        System.out.println("Extracted " + buildTimeProperties.size() + " BUILD_TIME properties to: " + outputFile);
    }

    private static void collectBuildTimeProperties(JsonNode node, Set<String> results) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            // Check if this node represents a property entry with phase == BUILD_TIME
            JsonNode phaseNode = node.has("phase") ? node.get("phase") : node.get("configPhase");
            JsonNode pathNode = node.has("path") ? node.get("path") : node.get("name");

            if (phaseNode != null && "BUILD_TIME".equals(phaseNode.asString()) && pathNode != null) {
                if (pathNode.has("property") && pathNode.get("property").isString()) {
                    String propKey = pathNode.get("property").asString().trim();
                    if (propKey.startsWith("quarkus.")) {
                        results.add(propKey);
                    }
                }
            }

            // Continue traversing nested sections, maps, and config roots
            node.forEach(entry -> collectBuildTimeProperties(entry, results));
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                collectBuildTimeProperties(item, results);
            }
        }
    }
}
