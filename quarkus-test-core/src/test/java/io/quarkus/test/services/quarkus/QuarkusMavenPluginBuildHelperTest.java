package io.quarkus.test.services.quarkus;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.bootstrap.BaseService;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.Configuration;
import io.quarkus.test.services.Dependency;

class QuarkusMavenPluginBuildHelperTest {

    @Test
    public void testFormatting() throws IOException {
        Dependency[] additionalBoms = new Dependency[] {
                createDependency("", "quarkus-mcp-server-bom", "${quarkus.platform.version}")
        };
        Path serviceFolder = Paths.get("target/QuarkusMavenPluginBuildHelperTest");
        ProdQuarkusApplicationManagedResourceBuilder builder = prepareBuilder(serviceFolder);
        builder.setBoms(additionalBoms);
        QuarkusMavenPluginBuildHelper helper = new QuarkusMavenPluginBuildHelper(builder);
        helper.prepareApplicationFolder();

        Path pom = serviceFolder.resolve("pom.xml");
        List<String> lines = Files.readAllLines(pom);
        int startline = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("<dependencyManagement>")) {
                startline = i;
                break;
            }
        }
        Assertions.assertNotEquals(0, startline, "No <dependencyManagement> tag in generated pom!");
        Assertions.assertEquals(2,
                lines.get(startline).indexOf('<'),
                "Unexpected indents size, please adjust the test!");
        // Check, that default import is in place and properly adjusted
        Assertions.assertEquals("        <artifactId>${quarkus.platform.artifact-id}</artifactId>",
                lines.get(startline + 4));
        Assertions.assertEquals("        <type>pom</type>", lines.get(startline + 6));
        Assertions.assertEquals("        <scope>import</scope>", lines.get(startline + 7));

        // custom import should have the same adjustment as the default one
        Assertions.assertEquals("      </dependency>", lines.get(startline + 8));
        Assertions.assertEquals("      <dependency>", lines.get(startline + 9));
        Assertions.assertEquals("        <groupId>${quarkus.platform.group-id}</groupId>", lines.get(startline + 10));
        Assertions.assertEquals("        <artifactId>quarkus-mcp-server-bom</artifactId>", lines.get(startline + 11));
        Assertions.assertEquals("        <version>${quarkus.platform.version}</version>", lines.get(startline + 12));
        Assertions.assertEquals("        <type>pom</type>", lines.get(startline + 13));
        Assertions.assertEquals("        <scope>import</scope>", lines.get(startline + 14));
    }

    @Test
    public void testDuplicateForcedDependenciesAreSkipped() throws IOException {
        Dependency dep = createDependency("io.quarkus", "quarkus-rest", "");
        Path serviceFolder = Paths.get("target/QuarkusMavenPluginBuildHelperDuplicateTest");
        ProdQuarkusApplicationManagedResourceBuilder builder = prepareBuilder(serviceFolder);
        builder.initForcedDependencies(new Dependency[] { dep, dep });
        QuarkusMavenPluginBuildHelper helper = new QuarkusMavenPluginBuildHelper(builder);
        helper.prepareApplicationFolder();

        Path pom = serviceFolder.resolve("pom.xml");
        List<String> lines = Files.readAllLines(pom);
        long count = lines.stream().filter(l -> l.contains("<artifactId>quarkus-rest</artifactId>")).count();
        Assertions.assertEquals(1, count, "Duplicate dependency found in generated POM");
    }

    @Test
    public void testConflictingVersionFails() {
        Dependency first = createDependency("io.quarkus", "quarkus-rest", "1.0.0");
        Dependency second = createDependency("io.quarkus", "quarkus-rest", "2.0.0");
        Path serviceFolder = Paths.get("target/QuarkusMavenPluginBuildHelperConflictTest");
        ProdQuarkusApplicationManagedResourceBuilder builder = prepareBuilder(serviceFolder);
        builder.initForcedDependencies(new Dependency[] { first, second });
        QuarkusMavenPluginBuildHelper helper = new QuarkusMavenPluginBuildHelper(builder);
        Assertions.assertThrows(IllegalArgumentException.class, helper::prepareApplicationFolder);
    }

    private static Dependency createDependency(String groupId, String artifactId, String version) {
        return new Dependency() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Dependency.class;
            }

            @Override
            public String groupId() {
                return groupId;
            }

            @Override
            public String artifactId() {
                return artifactId;
            }

            @Override
            public String version() {
                return version;
            }
        };
    }

    private ProdQuarkusApplicationManagedResourceBuilder prepareBuilder(Path serviceFolder) {
        ProdQuarkusApplicationManagedResourceBuilder builder = new ProdQuarkusApplicationManagedResourceBuilder();
        ServiceContext context = Mockito.mock(ServiceContext.class);
        Mockito.when(context.getServiceFolder()).thenReturn(serviceFolder);
        Service service = Mockito.mock(BaseService.class);
        Mockito.when(service.getConfiguration()).thenReturn(Configuration.load());
        Mockito.when(service.getProperty("quarkus.native.enabled")).thenReturn(Optional.of("false"));
        Mockito.when(context.getOwner()).thenReturn(service);

        builder.setContext(context);
        builder.initAppClasses(new Class[] { this.getClass() });
        return builder;
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.copy(Path.of("target/test-classes/quarkus-app-pom.xml").toAbsolutePath(), Path.of("target/quarkus-app-pom.xml"));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.delete(Path.of("target/quarkus-app-pom.xml"));
    }
}
